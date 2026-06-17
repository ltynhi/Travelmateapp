package com.example.travelmate.data.repository

import com.example.travelmate.data.model.Place
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PlaceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("Places")

    suspend fun getAllPlaces(): Result<List<Place>> {
        return try {
            val snapshot = collection.get().await()
            val places = snapshot.documents.mapNotNull { it.toObject(Place::class.java) }
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaceById(placeId: String): Result<Place> {
        return try {
            val doc = collection.document(placeId).get().await()
            val place = doc.toObject(Place::class.java) ?: throw Exception("Place not found")
            Result.success(place)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlacesByCategory(category: String): Result<List<Place>> {
        return try {
            val snapshot = collection.whereEqualTo("category", category).get().await()
            val places = snapshot.documents.mapNotNull { it.toObject(Place::class.java) }
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPlace(place: Place): Result<Place> {
        return try {
            val docRef = collection.document()
            val newPlace = place.copy(placeId = docRef.id)
            docRef.set(newPlace).await()
            Result.success(newPlace)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlace(place: Place): Result<Unit> {
        return try {
            collection.document(place.placeId).set(place).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlace(placeId: String): Result<Unit> {
        return try {
            collection.document(placeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaceRating(placeId: String, newRating: Double): Result<Unit> {
        return try {
            collection.document(placeId).update("rating", newRating).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
