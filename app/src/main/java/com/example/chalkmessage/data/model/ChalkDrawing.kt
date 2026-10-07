package com.example.chalkmessage.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChalkDrawing(
    @SerialName("id")
    val id: String? = null,

    @SerialName("board_id")
    val boardId: String,

    @SerialName("author_id")
    val authorId: String,

    @SerialName("author_name")
    val authorName: String,

    @SerialName("strokes")
    val strokes: List<Stroke>,

    @SerialName("created_at")
    val createdAt: String? = null
)
