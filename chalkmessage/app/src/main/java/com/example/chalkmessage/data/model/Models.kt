package com.example.chalkmessage.data.model

import kotlinx.serialization.Serializable

/**
 * A single point in a drawing stroke.
 * Like {x: number, y: number} in a canvas API.
 */
@Serializable
data class DrawPoint(
    val x: Float,
    val y: Float
)

/**
 * One continuous stroke (finger down -> move -> finger up).
 * Like a <path> element in SVG.
 */
@Serializable
data class Stroke(
    val points: List<DrawPoint>,
    val colorHex: String,   // Stored as "#FFFFFF" etc.
    val width: Float
)

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
