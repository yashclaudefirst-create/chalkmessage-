package com.example.chalkmessage.data.remote

import com.example.chalkmessage.data.model.ChalkMessage
import com.example.chalkmessage.data.model.Stroke
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FirestoreMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    // Firestore doesn't natively support nested arrays well, so store as JSON string
    val strokesJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDomain(message: ChalkMessage): FirestoreMessage {
            val json = Json { ignoreUnknownKeys = true }
            return FirestoreMessage(
                id = message.id,
                senderId = message.senderId,
                senderName = message.senderName,
                recipientId = message.recipientId,
                strokesJson = json.encodeToString(message.strokes),
                timestamp = message.timestamp
            )
        }
    }

    fun toDomain(): ChalkMessage {
        val json = Json { ignoreUnknownKeys = true }
        return ChalkMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            strokes = json.decodeFromString(strokesJson),
            timestamp = timestamp
        )
    }
}
