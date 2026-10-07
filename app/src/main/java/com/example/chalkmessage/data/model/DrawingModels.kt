package com.example.chalkmessage.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DrawPoint(val x: Float, val y: Float)

@Serializable
data class Stroke(
    val points: List<DrawPoint>,
    val colorHex: String,
    val width: Float
)
