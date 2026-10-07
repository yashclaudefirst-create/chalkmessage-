package com.example.chalkmessage.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://elttbxpvdeqcwxxcvsin.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVsdHRieHB2ZGVxY3d4eGN2c2luIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY0NzU5ODUsImV4cCI6MjEwMjA1MTk4NX0.KwB5ATuVdkflkKReRwB8klGXRL9Y3xlsnOZU8pIdZsc"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
