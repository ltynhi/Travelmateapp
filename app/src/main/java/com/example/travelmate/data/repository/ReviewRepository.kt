package com.example.travelmate.data.repository

import com.example.travelmate.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("Reviews")

    suspend fun getReviewsByPlace(placeId: String): Result<List<Review>> {
        return try {
            val snapshot = collection
                .whereEqualTo("placeId", placeId)
                .get().await()
            val reviews = snapshot.documents
                .mapNotNull { it.toObject(Review::class.java) }
                .sortedByDescending { it.timestamp } // sort local, không cần index
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllReviews(): Result<List<Review>> {
        return try {
            val snapshot = collection.get().await()
            val reviews = snapshot.documents
                .mapNotNull { it.toObject(Review::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReview(review: Review): Result<Unit> {
        return try {
            val docRef = collection.document()
            val newReview = review.copy(reviewId = docRef.id)
            docRef.set(newReview).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            collection.document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAverageRating(placeId: String): Double {
        return try {
            val snapshot = collection.whereEqualTo("placeId", placeId).get().await()
            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
            if (reviews.isEmpty()) 0.0
            else reviews.map { it.rating.toDouble() }.average()
        } catch (e: Exception) { 0.0 }
    }

    suspend fun getUserReviewForPlace(userId: String, placeId: String): Review? {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("placeId", placeId)
                .get().await()
            snapshot.documents.firstOrNull()?.toObject(Review::class.java)
        } catch (e: Exception) { null }
    }
}
