package com.example.travelmate.data.repository

import com.example.travelmate.data.model.TripMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TripChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("TripMessages")

    /**
     * Lắng nghe tin nhắn realtime.
     * KHÔNG dùng orderBy để tránh cần Firestore composite index.
     * Sort local theo createdAt.
     */
    fun getMessagesFlow(tripId: String): Flow<List<TripMessage>> = callbackFlow {
        val listener: ListenerRegistration = collection
            .whereEqualTo("tripId", tripId)
            // Bỏ orderBy — sort local để không cần index
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Không close flow khi lỗi — thử tiếp
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents
                    ?.mapNotNull { it.toObject(TripMessage::class.java) }
                    ?.sortedBy { it.createdAt }  // sort local
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(message: TripMessage): Result<Unit> {
        return try {
            val docRef = collection.document()
            docRef.set(message.copy(messageId = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            collection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
