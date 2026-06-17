package com.example.travelmate.data.repository

import com.example.travelmate.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("Users")

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = collection.get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val doc = collection.document(userId).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(userId: String, blocked: Boolean): Result<Unit> {
        return try {
            collection.document(userId).update("isBlocked", blocked).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalUsersCount(): Int {
        return try {
            collection.get().await().size()
        } catch (e: Exception) { 0 }
    }
}
