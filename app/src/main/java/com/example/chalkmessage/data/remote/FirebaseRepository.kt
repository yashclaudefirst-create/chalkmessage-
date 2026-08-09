package com.example.chalkmessage.data.remote

import com.example.chalkmessage.data.model.ChalkMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")

    /**
     * Send a message to Firestore.
     * Like POST /api/messages
     */
    suspend fun sendMessage(message: ChalkMessage) {
        val firestoreMsg = FirestoreMessage.fromDomain(message)
        messagesCollection.document(message.id).set(firestoreMsg).await()
    }

    /**
     * Listen for new messages sent TO a specific user.
     * Like a WebSocket subscription or Firebase Realtime DB listener.
     */
    fun listenForMessages(recipientId: String): Flow<List<ChalkMessage>> = callbackFlow {
        val subscription = messagesCollection
            .whereEqualTo("recipientId", recipientId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreMessage::class.java)?.toDomain()
                } ?: emptyList()
                trySend(messages)
            }

        // Cleanup when Flow collector stops
        awaitClose { subscription.remove() }
    }

    /**
     * Generate a unique invite code.
     * Like creating a referral code.
     */
    fun generateInviteCode(): String {
        return UUID.randomUUID().toString().take(8).uppercase()
    }
}
