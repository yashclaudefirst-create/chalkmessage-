package com.example.chalkmessage.data

import com.example.chalkmessage.data.local.MessageDao
import com.example.chalkmessage.data.local.MessageEntity
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.model.ChalkDrawing
import com.example.chalkmessage.data.model.ChalkMessage
import com.example.chalkmessage.data.model.Stroke
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

class ChalkRepository(
    private val messageDao: MessageDao,
    private val supabase: SupabaseClient,
    private val userPrefs: UserPrefs
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var realtimeJob: Job? = null

    val allMessages: Flow<List<ChalkMessage>> = messageDao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    val latestIncoming: Flow<ChalkMessage?> = messageDao.getLatestIncoming().map { it?.toDomain() }

    suspend fun getCurrentUserId(): String {
        return supabase.auth.currentUserOrNull()?.id
            ?: userPrefs.userId.first()
            ?: UUID.randomUUID().toString()
    }

    suspend fun sendMessage(strokes: List<Stroke>) {
        val currentUserId = getCurrentUserId()
        val userName = userPrefs.userName.first() ?: "Anonymous"
        val boardId = userPrefs.currentBoardId.first()

        if (boardId.isNullOrEmpty()) {
            throw IllegalArgumentException("No active board found. Please create or join a board.")
        }

        val drawing = ChalkDrawing(
            boardId = boardId,
            authorId = currentUserId,
            authorName = userName,
            strokes = strokes
        )

        // Save locally first (optimistic UI)
        val localId = UUID.randomUUID().toString()
        val localMessage = ChalkMessage(
            id = localId,
            senderId = currentUserId,
            senderName = userName,
            recipientId = boardId,
            strokes = strokes,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(localMessage.toEntity(isIncoming = false))

        // Save to Supabase chalk_drawings
        try {
            val inserted = supabase.postgrest["chalk_drawings"].insert(drawing) {
                select()
            }.decodeSingle<ChalkDrawing>()

            // Update local ID if returned from DB
            if (!inserted.id.isNullOrEmpty() && inserted.id != localId) {
                messageDao.deleteMessage(localId)
                val syncedMsg = inserted.toDomain()
                messageDao.insertMessage(syncedMsg.toEntity(isIncoming = false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep local copy even if network insert temporarily fails
        }
    }

    suspend fun loadDrawingsForBoard(boardId: String) {
        val currentUserId = getCurrentUserId()
        try {
            val drawings = supabase.postgrest["chalk_drawings"].select {
                filter {
                    eq("board_id", boardId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<ChalkDrawing>()

            drawings.forEach { drawing ->
                val domainMsg = drawing.toDomain()
                val isIncoming = (drawing.authorId != currentUserId)
                messageDao.insertMessage(domainMsg.toEntity(isIncoming = isIncoming))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRealtimeSync(boardId: String) {
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                // Initial load first
                loadDrawingsForBoard(boardId)

                // Subscribe to Realtime INSERT events
                val currentUserId = getCurrentUserId()
                val channel = supabase.channel("chalk_drawings_$boardId")
                val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "chalk_drawings"
                    filter = "board_id=eq.$boardId"
                }

                channel.subscribe()

                changeFlow.collect { action ->
                    try {
                        val drawing = action.decodeRecord<ChalkDrawing>()
                        val isIncoming = (drawing.authorId != currentUserId)
                        val domainMsg = drawing.toDomain()
                        messageDao.insertMessage(domainMsg.toEntity(isIncoming = isIncoming))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback reload if realtime subscription drops
                try {
                    loadDrawingsForBoard(boardId)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    suspend fun markAsRead(id: String) {
        messageDao.markAsRead(id)
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
    }

    suspend fun deleteMessageAndReturnBackup(id: String): Pair<ChalkMessage, Boolean>? {
        val entity = messageDao.getMessageById(id) ?: return null
        val domain = entity.toDomain()
        messageDao.deleteMessage(id)
        return Pair(domain, entity.isIncoming)
    }

    suspend fun getMessageById(id: String): ChalkMessage? {
        return messageDao.getMessageById(id)?.toDomain()
    }

    suspend fun insertMessage(message: ChalkMessage, isIncoming: Boolean) {
        messageDao.insertMessage(message.toEntity(isIncoming))
    }

    suspend fun syncIncomingMessages() {
        val boardId = userPrefs.currentBoardId.first()
        if (!boardId.isNullOrEmpty()) {
            loadDrawingsForBoard(boardId)
        }
    }

    suspend fun startRealtimeSyncForActiveBoard() {
        val boardId = userPrefs.currentBoardId.first()
        if (!boardId.isNullOrEmpty()) {
            startRealtimeSync(boardId)
        }
    }

    private fun ChalkDrawing.toDomain(): ChalkMessage {
        val ts = parseTimestamp(createdAt)
        return ChalkMessage(
            id = id ?: UUID.randomUUID().toString(),
            senderId = authorId,
            senderName = authorName,
            recipientId = boardId,
            strokes = strokes,
            timestamp = ts
        )
    }

    private fun parseTimestamp(createdAtIso: String?): Long {
        if (createdAtIso.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            Instant.parse(createdAtIso).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun ChalkMessage.toEntity(isIncoming: Boolean): MessageEntity {
        val json = Json { ignoreUnknownKeys = true }
        return MessageEntity(
            id = id,
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            strokesJson = json.encodeToString(strokes),
            timestamp = timestamp,
            isRead = isRead,
            isIncoming = isIncoming
        )
    }

    private fun MessageEntity.toDomain(): ChalkMessage {
        val json = Json { ignoreUnknownKeys = true }
        return ChalkMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            strokes = json.decodeFromString(strokesJson),
            timestamp = timestamp,
            isRead = isRead
        )
    }
}
