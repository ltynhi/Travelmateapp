package com.example.travelmate.data.repository

import com.example.travelmate.data.model.Place
import com.example.travelmate.data.model.Trip
import com.example.travelmate.data.model.TripPlace
import com.example.travelmate.data.model.TripPlaceWithDetail
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val tripsCollection = firestore.collection("Trips")
    private val tripPlacesCollection = firestore.collection("TripPlaces")
    private val placesCollection = firestore.collection("Places")

    // ── In-memory cache cho Places ────────────────────────────────────────────
    private val placeCache = mutableMapOf<String, Place>()

    // ─── Trips CRUD ──────────────────────────────────────────────────────────

    suspend fun getTripsByUser(userId: String): Result<List<Trip>> {
        return try {
            val snapshot = tripsCollection.whereEqualTo("userId", userId).get().await()
            val trips = snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lấy trip theo ID — dùng khi xem trip được mời */
    suspend fun getTripById(tripId: String): Result<Trip> {
        return try {
            val doc = tripsCollection.document(tripId).get().await()
            val trip = doc.toObject(Trip::class.java) ?: throw Exception("Trip not found")
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTrip(trip: Trip): Result<Trip> {
        return try {
            val docRef = tripsCollection.document()
            val newTrip = trip.copy(tripId = docRef.id)
            docRef.set(newTrip).await()
            Result.success(newTrip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            tripsCollection.document(trip.tripId).set(trip).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            // Xóa trip và tất cả TripPlaces bằng batch — nhanh hơn nhiều
            val tripPlacesSnap = tripPlacesCollection
                .whereEqualTo("tripId", tripId).get().await()

            val batch = firestore.batch()
            batch.delete(tripsCollection.document(tripId))
            tripPlacesSnap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── TripPlaces ──────────────────────────────────────────────────────────

    suspend fun addPlaceToTrip(
        tripId: String,
        placeId: String,
        visitDate: String = "",
        visitTime: String = "",
        note: String = ""
    ): Result<Unit> {
        return try {
            val currentCount = tripPlacesCollection
                .whereEqualTo("tripId", tripId).get().await().size()

            val docRef = tripPlacesCollection.document()
            val tripPlace = TripPlace(
                tripPlaceId = docRef.id,
                tripId = tripId,
                placeId = placeId,
                visitDate = visitDate,
                visitTime = visitTime,
                note = note,
                orderIndex = currentCount
            )
            // Batch: thêm TripPlace + cập nhật placeCount cùng lúc
            val batch = firestore.batch()
            batch.set(docRef, tripPlace)
            batch.update(tripsCollection.document(tripId), "placeCount", currentCount + 1)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTripPlace(tripPlace: TripPlace): Result<Unit> {
        return try {
            tripPlacesCollection.document(tripPlace.tripPlaceId).set(tripPlace).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePlaceFromTrip(tripPlaceId: String, tripId: String): Result<Unit> {
        return try {
            val count = tripPlacesCollection.whereEqualTo("tripId", tripId).get().await().size()
            val newCount = (count - 1).coerceAtLeast(0)

            val batch = firestore.batch()
            batch.delete(tripPlacesCollection.document(tripPlaceId))
            batch.update(tripsCollection.document(tripId), "placeCount", newCount)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách địa điểm kèm chi tiết.
     * Dùng coroutineScope + async để fetch tất cả places SONG SONG thay vì tuần tự.
     * Có cache để tránh fetch lại place đã biết.
     */
    suspend fun getTripPlacesWithDetail(tripId: String): Result<List<TripPlaceWithDetail>> {
        return try {
            val tripPlaces = tripPlacesCollection
                .whereEqualTo("tripId", tripId).get().await()
                .documents.mapNotNull { it.toObject(TripPlace::class.java) }

            if (tripPlaces.isEmpty()) return Result.success(emptyList())

            // Fetch tất cả places song song bằng coroutineScope
            val result: List<TripPlaceWithDetail> = coroutineScope {
                tripPlaces.map { tp ->
                    async {
                        val place = placeCache[tp.placeId]
                            ?: placesCollection.document(tp.placeId).get().await()
                                .toObject(Place::class.java)
                                ?.also { placeCache[tp.placeId] = it }
                        if (place != null) TripPlaceWithDetail(tp, place) else null
                    }
                }.awaitAll().filterNotNull()
            }

            val sorted = result.sortedWith(
                compareBy(
                    { parseDateForSort(it.tripPlace.visitDate) },
                    { parseTimeForSort(it.tripPlace.visitTime) },
                    { it.tripPlace.orderIndex }
                )
            )
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalTripsCount(): Int {
        return try {
            tripsCollection.get().await().size()
        } catch (e: Exception) { 0 }
    }

    private fun parseDateForSort(date: String): Long {
        if (date.isBlank()) return Long.MAX_VALUE
        return try {
            val parts = date.split("/")
            if (parts.size == 3) "${parts[2]}${parts[1].padStart(2,'0')}${parts[0].padStart(2,'0')}".toLong()
            else Long.MAX_VALUE
        } catch (e: Exception) { Long.MAX_VALUE }
    }

    private fun parseTimeForSort(time: String): Int {
        if (time.isBlank()) return Int.MAX_VALUE
        return try {
            val parts = time.split(":")
            if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt()
            else Int.MAX_VALUE
        } catch (e: Exception) { Int.MAX_VALUE }
    }
}
