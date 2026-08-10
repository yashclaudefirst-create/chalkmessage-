package com.example.chalkmessage.data

import com.example.chalkmessage.data.local.MessageDao
import com.example.chalkmessage.data.local.MessageEntity
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.model.ChalkMessage
import com.example.chalkmessage.data.model.Stroke
import com.example.chalkmessage.data.remote.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ChalkRepository(
    private val messageDao: MessageDao,
    private val firebaseRepo: FirebaseRepository,
    private val userPrefs: UserPrefs
) {
    val allMessages: Flow<List<ChalkMessage>> = messageDao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    val latestIncoming: Flow<ChalkMessage?> = messageDao.getLatestIncoming().map { it?.toDomain() }

    suspend fun sendMessage(strokes: List<Stroke>) {
        val myId = userPrefs.userId.first() ?: return
        val myName = userPrefs.userName.first() ?: "Anonymous"
        val connectedTo = userPrefs.connectedTo.first() ?: return

        // For MVP: send to first connection only
        val recipientId = connectedTo.split(",").firstOrNull() ?: return

        val message = ChalkMessage(
            id = java.util.UUID.randomUUID().toString(),
            senderId = myId,
            senderName = myName,
            recipientId = recipientId,
            strokes = strokes,
            timestamp = System.currentTimeMillis()
        )

        // Save locally first (optimistic UI)
        messageDao.insertMessage(message.toEntity(isIncoming = false))
        // Then sync to cloud
        firebaseRepo.sendMessage(message)
    }

    suspend fun syncIncomingMessages() {
        val myId = userPrefs.userId.first() ?: return
        firebaseRepo.listenForMessages(myId).collect { messages ->
            messages.forEach { msg ->
                messageDao.insertMessage(msg.toEntity(isIncoming = true))
            }
        }
    }

    suspend fun markAsRead(id: String) {
        messageDao.markAsRead(id)
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
