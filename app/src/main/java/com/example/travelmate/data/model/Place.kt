package com.example.travelmate.data.model

data class Place(
    val placeId: String = "",
    val name: String = "",
    val imageUrl: String = "",          // Ảnh đại diện (ảnh đầu tiên)
    val images: List<String> = emptyList(), // Gallery nhiều ảnh
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val category: String = "",
    val rating: Double = 0.0
) {
    /** Lấy tất cả ảnh — gộp imageUrl + images, bỏ trùng */
    fun getAllImages(): List<String> {
        val all = mutableListOf<String>()
        if (imageUrl.isNotBlank()) all.add(imageUrl)
        images.forEach { if (it.isNotBlank() && it != imageUrl) all.add(it) }
        return all.distinct()
    }
}
