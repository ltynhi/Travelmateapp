package com.example.travelmate.data.model

data class TripPlace(
    val tripPlaceId: String = "",
    val tripId: String = "",
    val placeId: String = "",        // Trống nếu là địa điểm tự nhập
    val visitDate: String = "",      // "dd/MM/yyyy"
    val visitTime: String = "",      // "HH:mm"
    val note: String = "",           // Ghi chú cho địa điểm này
    val estimatedCost: Long = 0L,    // Chi phí ước tính (VNĐ)
    val orderIndex: Int = 0,         // Thứ tự trong ngày
    // ── Địa điểm tự nhập (không có trong database) ───────────────────────────
    val customName: String = "",     // Tên địa điểm tự nhập
    val customAddress: String = "",  // Địa chỉ tự nhập
    val customCategory: String = "", // Loại địa điểm tự chọn
    val customImageUrl: String = ""  // Ảnh URL tự nhập (tuỳ chọn)
) {
    /** Kiểm tra đây có phải địa điểm tự nhập không */
    val isCustom: Boolean get() = placeId.isBlank() && customName.isNotBlank()
}
