package com.example.chalkmessage.data.remote

import com.example.chalkmessage.data.model.ChalkMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")
    private val usersCollection = db.collection("users")
    private val inviteCodesCollection = db.collection("inviteCodes")
    private val connectionsCollection = db.collection("connections")

    /**
     * Send a message to Firestore.
     */
    suspend fun sendMessage(message: ChalkMessage) {
        val firestoreMsg = FirestoreMessage.fromDomain(message)
        messagesCollection.document(message.id).set(firestoreMsg).await()

        // Update lastMessageAt inside the connections document
        try {
            updateConnectionLastMessage(message.senderId, message.recipientId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Listen for new messages sent TO a specific user.
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

        awaitClose { subscription.remove() }
    }

    /**
     * Generate a unique 8-character invite code.
     */
    fun generateInviteCode(): String {
        return UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
    }

    /**
     * Create user profile in Firestore
     */
    suspend fun createUserProfile(userId: String, name: String, inviteCode: String, fcmToken: String) {
        val userMap = hashMapOf(
            "name" to name,
            "inviteCode" to inviteCode,
            "fcmToken" to fcmToken,
            "createdAt" to System.currentTimeMillis()
        )
        usersCollection.document(userId).set(userMap).await()

        // Store reverse lookup pointing to userId
        val lookupMap = hashMapOf(
            "userId" to userId
        )
        inviteCodesCollection.document(inviteCode).set(lookupMap).await()
    }

    /**
     * Look up invite code
     */
    suspend fun lookupInviteCode(enteredCode: String): String? {
        val doc = inviteCodesCollection.document(enteredCode.uppercase().trim()).get().await()
        return if (doc.exists()) {
            doc.getString("userId")
        } else {
            null
        }
    }

    /**
     * Create connection document where connectionId is smallerUserId_largerUserId alphabetically sorted
     */
    suspend fun createConnection(myUserId: String, partnerUserId: String) {
        val (user1, user2) = if (myUserId < partnerUserId) {
            Pair(myUserId, partnerUserId)
        } else {
            Pair(partnerUserId, myUserId)
        }
        val connectionId = "${user1}_${user2}"
        val connectionMap = hashMapOf(
            "user1" to user1,
            "user2" to user2,
            "createdAt" to System.currentTimeMillis(),
            "lastMessageAt" to System.currentTimeMillis()
        )
        connectionsCollection.document(connectionId).set(connectionMap).await()
    }

    /**
     * Update last message timestamp in connection
     */
    private suspend fun updateConnectionLastMessage(senderId: String, recipientId: String) {
        val (user1, user2) = if (senderId < recipientId) {
            Pair(senderId, recipientId)
        } else {
            Pair(recipientId, senderId)
        }
        val connectionId = "${user1}_${user2}"
        connectionsCollection.document(connectionId).update("lastMessageAt", System.currentTimeMillis()).await()
    }

    /**
     * Get user FCM token robustly
     */
    suspend fun getFcmToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            ""
        }
    }
}
