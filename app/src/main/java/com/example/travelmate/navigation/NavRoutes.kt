package com.example.travelmate.navigation

object NavRoutes {
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PLACE_DETAIL = "place_detail/{placeId}"
    const val FAVORITES = "favorites"
    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val TRIP_INVITE = "trip_invite/{tripId}"
    const val TRIP_INVITE_INBOX = "trip_invite_inbox"
    const val TRIP_CHAT = "trip_chat/{tripId}"
    const val TIMELINE = "timeline"
    const val CREATE_POST = "create_post"

    fun createPost(tripId: String = "", tripName: String = ""): String {
        return if (tripId.isBlank()) "create_post"
        else {
            val encodedName = java.net.URLEncoder.encode(tripName, "UTF-8")
            "create_post?tripId=$tripId&tripName=$encodedName"
        }
    }
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PLACES = "admin_places"
    const val ADMIN_ADD_PLACE = "admin_add_place"
    const val ADMIN_EDIT_PLACE = "admin_edit_place/{placeId}"
    const val ADMIN_TIMELINE = "admin_timeline"
    const val ADMIN_REVIEWS = "admin_reviews"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_NOTIFICATIONS = "admin_notifications"

    fun placeDetail(placeId: String) = "place_detail/$placeId"
    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    fun tripInvite(tripId: String) = "trip_invite/$tripId"
    fun tripChat(tripId: String) = "trip_chat/$tripId"
    fun adminEditPlace(placeId: String) = "admin_edit_place/$placeId"
}
