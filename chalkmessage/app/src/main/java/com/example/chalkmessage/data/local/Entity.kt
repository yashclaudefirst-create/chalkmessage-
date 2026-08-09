package com.example.chalkmessage.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "messages")
@TypeConverters(StrokeConverter::class)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    // Stored as JSON string in the database
    val strokesJson: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isIncoming: Boolean  // true = received, false = sent
)
