package com.example.travelmate.data.repository

import com.example.travelmate.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("Notifications")

    /** Lấy thông báo dành cho user (gửi tất cả + gửi riêng cho user đó) */
    suspend fun getNotificationsForUser(userId: String): Result<List<AppNotification>> {
        return try {
            val allSnapshot = collection.whereEqualTo("targetUserId", "").get().await()
            val personalSnapshot = collection.whereEqualTo("targetUserId", userId).get().await()

            val all = allSnapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) }
            val personal = personalSnapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) }

            val combined = (all + personal)
                .distinctBy { it.notificationId }
                .sortedByDescending { it.createdAt }

            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllNotifications(): Result<List<AppNotification>> {
        return try {
            val snapshot = collection.get().await()
            val list = snapshot.documents
                .mapNotNull { it.toObject(AppNotification::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin tạo thông báo mới */
    suspend fun createNotification(notification: AppNotification): Result<Unit> {
        return try {
            val docRef = collection.document()
            docRef.set(notification.copy(notificationId = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin xóa thông báo */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            collection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** User đánh dấu đã đọc */
    suspend fun markAsRead(notificationId: String, userId: String): Result<Unit> {
        return try {
            val doc = collection.document(notificationId).get().await()
            val notification = doc.toObject(AppNotification::class.java) ?: return Result.success(Unit)
            if (!notification.readBy.contains(userId)) {
                val updatedReadBy = notification.readBy + userId
                collection.document(notificationId)
                    .update("readBy", updatedReadBy).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đếm số thông báo chưa đọc */
    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val result = getNotificationsForUser(userId).getOrNull() ?: return 0
            result.count { !it.readBy.contains(userId) }
        } catch (e: Exception) { 0 }
    }
}
