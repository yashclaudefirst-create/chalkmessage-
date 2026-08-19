package com.example.chalkmessage.data

import com.example.chalkmessage.data.remote.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class BoardMemberUi(
    val userId: String,
    val name: String
)

@Serializable
data class BoardDto(
    val id: String? = null,
    val name: String,
    val code: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BoardMemberDto(
    val id: String? = null,
    @SerialName("board_id") val boardId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val name: String? = null
)

@Serializable
data class BoardMemberWithUserDto(
    @SerialName("user_id") val userId: String,
    val users: UserDto? = null
)

@Serializable
data class DrawingDto(
    val id: String? = null,
    @SerialName("board_id") val boardId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String,
    @SerialName("user_color") val userColor: String,
    @SerialName("strokes_json") val strokesJson: String,
    val caption: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("self_destruct_at") val selfDestructAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LikeDto(
    val id: String? = null,
    @SerialName("drawing_id") val drawingId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommentDto(
    val id: String? = null,
    @SerialName("drawing_id") val drawingId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

class BoardRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {
    suspend fun createBoard(name: String): BoardDto {
        return client.from("boards").insert(mapOf("name" to name)) {
            select()
        }.decodeSingle<BoardDto>()
    }

    suspend fun joinBoardByCode(code: String): BoardMemberDto {
        // Resolve code to board_id first
        val board = client.from("boards")
            .select { filter { eq("code", code) } }
            .decodeSingle<BoardDto>()

        val boardId = board.id ?: throw IllegalStateException("Board ID not found for code $code")

        return client.from("board_members").insert(mapOf("board_id" to boardId)) {
            select()
        }.decodeSingle<BoardMemberDto>()
    }

    suspend fun getMyBoards(): List<BoardDto> {
        return client.from("boards").select().decodeList<BoardDto>()
    }

    suspend fun getBoardMembers(boardId: String): List<BoardMemberUi> {
        return try {
            val response = client.from("board_members")
                .select(columns = Columns.raw("user_id, users(id, name)")) {
                    filter { eq("board_id", boardId) }
                }
                .decodeList<BoardMemberWithUserDto>()

            response.map { member ->
                BoardMemberUi(
                    userId = member.userId,
                    name = member.users?.name ?: member.userId.take(4)
                )
            }
        } catch (e: Exception) {
            val members = client.from("board_members")
                .select { filter { eq("board_id", boardId) } }
                .decodeList<BoardMemberDto>()
            members.map { BoardMemberUi(userId = it.userId, name = it.userId.take(4)) }
        }
    }

    suspend fun getDrawings(boardId: String): List<DrawingDto> {
        return client.from("drawings")
            .select {
                filter { eq("board_id", boardId) }
            }
            .decodeList<DrawingDto>()
    }

    suspend fun listenForNewDrawings(boardId: String): Flow<DrawingDto> {
        val channel = client.channel("drawings_channel_$boardId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "drawings"
            filter = "board_id=eq.$boardId"
        }
        channel.subscribe()
        return changeFlow.map { action ->
            Json.decodeFromJsonElement<DrawingDto>(action.record)
        }
    }

    suspend fun postDrawing(
        boardId: String,
        userName: String,
        userColor: String,
        strokesJson: String,
        caption: String? = null
    ): DrawingDto {
        val drawing = DrawingDto(
            boardId = boardId,
            userName = userName,
            userColor = userColor,
            strokesJson = strokesJson,
            caption = caption
        )
        return client.from("drawings").insert(drawing) {
            select()
        }.decodeSingle<DrawingDto>()
    }

    suspend fun toggleLike(drawingId: String) {
        val existing = client.from("likes")
            .select { filter { eq("drawing_id", drawingId) } }
            .decodeList<LikeDto>()

        if (existing.isNotEmpty()) {
            client.from("likes").delete { filter { eq("drawing_id", drawingId) } }
        } else {
            client.from("likes").insert(mapOf("drawing_id" to drawingId))
        }
    }

    suspend fun addComment(drawingId: String, content: String, userName: String? = null): CommentDto {
        val comment = mapOf(
            "drawing_id" to drawingId,
            "content" to content,
            "user_name" to userName
        )
        return client.from("comments").insert(comment) {
            select()
        }.decodeSingle<CommentDto>()
    }

    suspend fun getComments(drawingId: String): List<CommentDto> {
        return client.from("comments")
            .select { filter { eq("drawing_id", drawingId) } }
            .decodeList<CommentDto>()
    }
}
