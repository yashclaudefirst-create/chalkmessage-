package com.example.chalkmessage

import android.app.Application
import androidx.room.Room
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.local.AppDatabase
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.remote.FirebaseRepository

class ChalkMessageApp : Application() {
    // Lazy initialization: created only when first accessed
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "chalk_database"
        ).build()
    }

    val userPrefs by lazy { UserPrefs(applicationContext) }
    val firebaseRepo by lazy { FirebaseRepository() }

    val repository by lazy {
        ChalkRepository(
            messageDao = database.messageDao(),
            firebaseRepo = firebaseRepo,
            userPrefs = userPrefs
        )
    }
}
