package com.example.chalkmessage.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {
    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            ChalkWidget().updateAll(context)
        }
    }
}
