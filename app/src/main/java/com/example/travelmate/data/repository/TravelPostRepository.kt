package com.example.travelmate.data.repository

import com.example.travelmate.data.model.TravelPost
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TravelPostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("TravelPosts")

    suspend fun getAllPosts(): Result<List<TravelPost>> {
        return try {
            val snapshot = collection.get().await()
            val posts = snapshot.documents
                .mapNotNull { it.toObject(TravelPost::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByUser(userId: String): Result<List<TravelPost>> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .get().await()
            val posts = snapshot.documents
                .mapNotNull { it.toObject(TravelPost::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(post: TravelPost): Result<Unit> {
        return try {
            val docRef = collection.document()
            val newPost = post.copy(postId = docRef.id)
            docRef.set(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            collection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, caption: String, location: String, imageUrl: String): Result<Unit> {
        return try {
            collection.document(postId).update(
                mapOf(
                    "caption" to caption,
                    "location" to location,
                    "imageUrl" to imageUrl
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalPostsCount(): Int {
        return try {
            collection.get().await().size()
        } catch (e: Exception) { 0 }
    }
}
