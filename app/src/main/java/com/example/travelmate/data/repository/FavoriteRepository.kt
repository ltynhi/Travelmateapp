package com.example.travelmate.data.repository

import com.example.travelmate.data.model.Favorite
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FavoriteRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("Favorites")

    suspend fun getFavoritesByUser(userId: String): Result<List<Favorite>> {
        return try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val favorites = snapshot.documents.mapNotNull { it.toObject(Favorite::class.java) }
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(userId: String, placeId: String): Result<Unit> {
        return try {
            val docRef = collection.document()
            val favorite = Favorite(
                favoriteId = docRef.id,
                userId = userId,
                placeId = placeId
            )
            docRef.set(favorite).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(userId: String, placeId: String): Result<Unit> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("placeId", placeId)
                .get().await()
            snapshot.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(userId: String, placeId: String): Boolean {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("placeId", placeId)
                .get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
