package com.example.chalkmessage.data.remote

import com.example.chalkmessage.data.model.Board
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.security.SecureRandom
import java.time.Instant
import java.time.format.DateTimeFormatter

class BoardRepository(
    private val supabase: SupabaseClient
) {
    private val random = SecureRandom()

    suspend fun createBoard(boardName: String, yourName: String): Board {
        val session = supabase.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Active authentication session required to create a board.")
        val uid = session.user?.id
            ?: throw IllegalStateException("User ID not found in current authentication session.")

        var selectedCode = ""
        var attempts = 0
        var isUnique = false

        while (!isUnique && attempts < 5) {
            attempts++
            val candidateCode = (100000 + random.nextInt(900000)).toString()
            val nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

            // Check uniqueness against active (non-expired) codes
            val existing = supabase.postgrest["boards"].select {
                filter {
                    eq("code", candidateCode)
                    gt("code_expires_at", nowIso)
                }
            }.decodeList<Board>()

            if (existing.isEmpty()) {
                selectedCode = candidateCode
                isUnique = true
            }
        }

        if (!isUnique) {
            throw IllegalStateException("Failed to generate a unique code after multiple attempts. Please try again.")
        }

        val now = Instant.now()
        val expiresAt = now.plusSeconds(24 * 60 * 60)
        val nowIso = DateTimeFormatter.ISO_INSTANT.format(now)
        val expiresAtIso = DateTimeFormatter.ISO_INSTANT.format(expiresAt)

        val newBoard = Board(
            name = boardName,
            code = selectedCode,
            createdBy = uid,
            createdByName = yourName,
            memberIds = listOf(uid),
            createdAt = nowIso,
            codeExpiresAt = expiresAtIso
        )

        // TODO: Rate-limit code lookups at join (step 3) via Postgres RPC + attempt counter.

        val inserted = supabase.postgrest["boards"].insert(newBoard) {
            select()
        }.decodeSingle<Board>()

        return inserted
    }
}
