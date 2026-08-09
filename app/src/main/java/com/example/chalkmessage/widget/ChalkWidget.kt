package com.example.chalkmessage.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withSave
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
import androidx.glance.text.Text
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
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(androidx.glance.unit.ColorProvider(Color(0xFF1B5E20)))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(8.dp),
        contentAlignment = androidx.glance.layout.Alignment.Center
    ) {
        if (strokesJson != null) {
            // Render strokes to bitmap for widget display
            val bitmap = rememberStrokesBitmap(context, strokesJson)
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = "Chalk message from $senderName",
                    modifier = GlanceModifier.fillMaxSize()
                )
            } else {
                Text(text = "Tap to view message")
            }
        } else {
            Text(text = "No messages yet")
        }
    }
}

/**
 * Converts stored stroke JSON to a Bitmap that Glance can display.
 * Like rendering an offscreen canvas in HTML5.
 */
@Composable
private fun rememberStrokesBitmap(context: Context, strokesJson: String): Bitmap? {
    return androidx.compose.runtime.remember(strokesJson) {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val strokes: List<Stroke> = json.decodeFromString(strokesJson)

            val width = 800
            val height = 800
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fill background
            canvas.drawColor(android.graphics.Color.parseColor("#1B5E20"))

            val paint = Paint().apply {
                isAntiAlias = true
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                style = Paint.Style.STROKE
            }

            canvas.withSave {
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach

                    val path = Path().apply {
                        val first = stroke.points.first()
                        moveTo(first.x, first.y)
                        for (i in 1 until stroke.points.size) {
                            val point = stroke.points[i]
                            val prev = stroke.points[i - 1]
                            val midX = (prev.x + point.x) / 2
                            val midY = (prev.y + point.y) / 2
                            quadTo(prev.x, prev.y, midX, midY)
                        }
                    }

                    paint.color = android.graphics.Color.parseColor(stroke.colorHex)
                    paint.strokeWidth = stroke.width
                    paint.alpha = 230
                    canvas.drawPath(path, paint)

                    // Texture overlay
                    paint.strokeWidth = stroke.width * 0.7f
                    paint.alpha = 80
                    canvas.drawPath(path, paint)
                }
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
