package com.example.chalkmessage.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Board(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("name")
    val name: String,

    @SerialName("code")
    val code: String,

    @SerialName("created_by")
    val createdBy: String,

    @SerialName("created_by_name")
    val createdByName: String,

    @SerialName("member_ids")
    val memberIds: List<String>,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("code_expires_at")
    val codeExpiresAt: String
)
