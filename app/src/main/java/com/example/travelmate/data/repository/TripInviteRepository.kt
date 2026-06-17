package com.example.travelmate.data.repository

import com.example.travelmate.data.model.TripInvite
import com.example.travelmate.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TripInviteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val invitesCol = db.collection("TripInvites")
    private val usersCol = db.collection("Users")
    private val tripsCol = db.collection("Trips")

    /** Tìm user theo email */
    suspend fun findUserByEmail(email: String): Result<User?> {
        return try {
            val snap = usersCol.whereEqualTo("email", email.trim()).get().await()
            val user = snap.documents.firstOrNull()?.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Tìm user theo tên (tìm gần đúng) */
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snap = usersCol.get().await()
            val users = snap.documents
                .mapNotNull { it.toObject(User::class.java) }
                .filter {
                    it.fullName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
                }
                .filter { it.role != "admin" }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Gửi lời mời */
    suspend fun sendInvite(invite: TripInvite): Result<Unit> {
        return try {
            // Kiểm tra đã mời chưa
            val existing = invitesCol
                .whereEqualTo("tripId", invite.tripId)
                .whereEqualTo("toUserId", invite.toUserId)
                .whereEqualTo("status", "pending")
                .get().await()
            if (!existing.isEmpty) return Result.failure(Exception("Đã gửi lời mời cho người này rồi"))

            val docRef = invitesCol.document()
            docRef.set(invite.copy(inviteId = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lấy lời mời đang chờ của user */
    suspend fun getPendingInvites(userId: String): Result<List<TripInvite>> {
        return try {
            val snap = invitesCol
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get().await()
            val invites = snap.documents
                .mapNotNull { it.toObject(TripInvite::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(invites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Chấp nhận lời mời → thêm vào memberIds của Trip */
    suspend fun acceptInvite(invite: TripInvite): Result<Unit> {
        return try {
            // Cập nhật status invite
            invitesCol.document(invite.inviteId).update("status", "accepted").await()

            // Thêm userId vào memberIds của Trip
            val tripDoc = tripsCol.document(invite.tripId).get().await()
            val currentMembers = tripDoc.get("memberIds") as? List<*> ?: emptyList<String>()
            val updatedMembers = (currentMembers.filterIsInstance<String>() + invite.toUserId).distinct()
            tripsCol.document(invite.tripId).update("memberIds", updatedMembers).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Từ chối lời mời */
    suspend fun declineInvite(inviteId: String): Result<Unit> {
        return try {
            invitesCol.document(inviteId).update("status", "declined").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lấy danh sách thành viên của trip */
    suspend fun getTripMembers(memberIds: List<String>): Result<List<User>> {
        if (memberIds.isEmpty()) return Result.success(emptyList())
        return try {
            val members = memberIds.mapNotNull { uid ->
                usersCol.document(uid).get().await().toObject(User::class.java)
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Rời khỏi chuyến đi */
    suspend fun leaveTrip(tripId: String, userId: String): Result<Unit> {
        return try {
            val tripDoc = tripsCol.document(tripId).get().await()
            val currentMembers = tripDoc.get("memberIds") as? List<*> ?: emptyList<String>()
            val updatedMembers = currentMembers.filterIsInstance<String>().filter { it != userId }
            tripsCol.document(tripId).update("memberIds", updatedMembers).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lấy trips mà user được mời tham gia */
    suspend fun getJoinedTrips(userId: String): Result<List<com.example.travelmate.data.model.Trip>> {
        return try {
            val snap = tripsCol
                .whereArrayContains("memberIds", userId)
                .get().await()
            val trips = snap.documents.mapNotNull {
                it.toObject(com.example.travelmate.data.model.Trip::class.java)
            }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đếm lời mời chưa đọc */
    suspend fun getPendingInviteCount(userId: String): Int {
        return try {
            invitesCol
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get().await().size()
        } catch (e: Exception) { 0 }
    }
}
