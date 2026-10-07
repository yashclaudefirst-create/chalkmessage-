package com.example.chalkmessage.data.model

import kotlinx.serialization.Serializable

/**
 * A complete chalk message.
 * Think of it as a "post" containing an array of SVG paths.
 */
@Serializable
data class ChalkMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val strokes: List<Stroke> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
