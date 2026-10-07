package com.example.chalkmessage

import android.app.Application
import androidx.room.Room
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.local.AppDatabase
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.remote.BoardRepository
import com.example.chalkmessage.data.remote.FirebaseRepository
import com.example.chalkmessage.data.remote.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

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

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    val boardRepository by lazy { BoardRepository(supabase) }

    val repository by lazy {
        ChalkRepository(
            messageDao = database.messageDao(),
            supabase = supabase,
            userPrefs = userPrefs
        )
    }
}
