package com.example.travelmate.data.repository

/**
 * StorageRepository đã được thay thế bằng nhập URL ảnh trực tiếp.
 * Firebase Storage yêu cầu gói Blaze (trả phí) nên không sử dụng.
 * Người dùng nhập link ảnh từ internet (Unsplash, Google Photos, v.v.)
 */
class StorageRepository {
    // Không còn dùng Firebase Storage
    // Giữ class này để tránh lỗi import ở các file khác
}
