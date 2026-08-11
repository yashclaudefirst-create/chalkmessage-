package com.example.chalkmessage.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.example.chalkmessage.MainActivity
import com.example.chalkmessage.data.local.AppDatabase
import com.example.chalkmessage.data.model.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChalkWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load latest message from Room (widgets can't use Flow directly, so we fetch once)
        val database = AppDatabase.getInstance(context)
        val latestMessage = withContext(Dispatchers.IO) {
            database.messageDao().getLatestIncoming().first()
        }

        provideContent {
            WidgetContent(
                context = context,
                strokesJson = latestMessage?.strokesJson,
                senderName = latestMessage?.senderName ?: "No messages yet"
            )
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    strokesJson: String?,
    senderName: String
) {
    // Generate bitmap on-demand from JSON
    val bitmap = rememberStrokesBitmap(context, strokesJson, senderName)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(androidx.glance.unit.ColorProvider(Color(0xFF0D1B0D).hashCode())) // deep chalkboard background
            .clickable(actionStartActivity(MainActivity::class.java))
            .padding(4.dp),
        contentAlignment = androidx.glance.layout.Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Chalk message from $senderName",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

/**
 * Converts stored stroke JSON or renders a beautiful empty state to a Bitmap that Glance can display.
 */
@Composable
private fun rememberStrokesBitmap(context: Context, strokesJson: String?, senderName: String): Bitmap? {
    return androidx.compose.runtime.remember(strokesJson, senderName) {
        try {
            val width = 800
            val height = 800
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Fill background with deep chalkboard green #0D1B0D
            canvas.drawColor(android.graphics.Color.parseColor("#0D1B0D"))

            // 2. Draw chalkboard dust/noise texture
            val random = java.util.Random(1337)
            val noisePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 15 // very subtle dust
            }
            for (i in 0 until 4000) {
                val rx = random.nextFloat() * width
                val ry = random.nextFloat() * height
                val rsize = 1f + random.nextFloat() * 1.5f
                canvas.drawCircle(rx, ry, rsize, noisePaint)
            }

            // 3. Draw a classic chalkboard border
            val borderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1B5E20")
                style = Paint.Style.STROKE
                strokeWidth = 20f
            }
            canvas.drawRect(10f, 10f, width - 10f, height - 10f, borderPaint)

            if (strokesJson != null) {
                // Parse strokes
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val strokes: List<Stroke> = json.decodeFromString(strokesJson)

                val paint = Paint().apply {
                    isAntiAlias = true
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    style = Paint.Style.STROKE
                }

                // Render strokes scaled down slightly and centered
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach

                    val path = Path().apply {
                        val scaleFactor = 0.8f
                        val offsetX = (width * (1f - scaleFactor)) / 2f
                        val offsetY = (height * (1f - scaleFactor)) / 2f

                        val first = stroke.points.first()
                        moveTo(first.x * scaleFactor + offsetX, first.y * scaleFactor + offsetY)
                        for (i in 1 until stroke.points.size) {
                            val point = stroke.points[i]
                            val prev = stroke.points[i - 1]
                            val midX = ((prev.x + point.x) / 2) * scaleFactor + offsetX
                            val midY = ((prev.y + point.y) / 2) * scaleFactor + offsetY
                            quadTo(prev.x * scaleFactor + offsetX, prev.y * scaleFactor + offsetY, midX, midY)
                        }
                    }

                    // Render with soft double-pass chalk glow
                    paint.color = android.graphics.Color.parseColor(stroke.colorHex)
                    paint.strokeWidth = stroke.width * 0.8f
                    paint.alpha = 230
                    canvas.drawPath(path, paint)

                    paint.strokeWidth = stroke.width * 0.8f * 0.7f
                    paint.alpha = 90
                    canvas.drawPath(path, paint)
                }

                // Draw Sender Name Header
                val headerPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#FFF176") // chalk yellow
                    textSize = 38f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                }
                canvas.drawText("From: $senderName", (width / 2).toFloat(), 70f, headerPaint)

                // Draw Tap to Open hint at the bottom
                val hintPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#F5F5F5") // chalk white
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                }
                canvas.drawText("✨ Tap to open Drawn to You ✨", (width / 2).toFloat(), (height - 50).toFloat(), hintPaint)

            } else {
                // RENDER BEAUTIFUL CHALK EMPTY STATE
                // Draw cute heart outline in center
                val paint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#F48FB1") // pink chalk
                    strokeWidth = 12f
                    style = Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
                val heartPath = Path().apply {
                    moveTo(400f, 440f)
                    cubicTo(300f, 340f, 240f, 240f, 400f, 180f)
                    cubicTo(560f, 240f, 500f, 340f, 400f, 440f)
                }
                canvas.drawPath(heartPath, paint)

                // Draw Text Preview Details
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#FFF176") // chalk yellow
                    textSize = 42f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                }
                canvas.drawText("No messages yet", (width / 2).toFloat(), 530f, textPaint)

                val subtextPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#F5F5F5") // chalk white
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                }
                canvas.drawText("Tap to write a chalk message!", (width / 2).toFloat(), 600f, subtextPaint)
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
