package com.example.travelmate.utils

import com.example.travelmate.data.model.Place
import com.example.travelmate.data.model.Review
import com.example.travelmate.data.model.TravelPost
import com.example.travelmate.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

object SeedData {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Timeout cho mỗi thao tác Firestore (ms)
    private const val OP_TIMEOUT = 8_000L

    private val samplePlaces = listOf(
        // ── ĐÀ NẴNG ──────────────────────────────────────────────────────────
        Place(
            name = "Bãi biển Mỹ Khê",
            imageUrl = "https://images.unsplash.com/photo-1559628376-f3fe5f782a2e?w=800",
            description = "Bãi biển Mỹ Khê là một trong những bãi biển đẹp nhất hành tinh theo bình chọn của Forbes, với bờ cát trắng trải dài hơn 900m.",
            address = "Phước Mỹ, Sơn Trà, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Biển",
            rating = 4.8
        ),
        Place(
            name = "Cầu Rồng",
            imageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=800",
            description = "Cầu Rồng là công trình biểu tượng của Đà Nẵng, được thiết kế hình rồng phun lửa và nước vào cuối tuần.",
            address = "Cầu Rồng, Hải Châu, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Check-in",
            rating = 4.7
        ),
        Place(
            name = "Bà Nà Hills",
            imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
            description = "Bà Nà Hills là khu du lịch nghỉ dưỡng nổi tiếng với Cầu Vàng, cáp treo dài nhất thế giới và làng Pháp cổ kính.",
            address = "Hòa Ninh, Hòa Vang, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Núi",
            rating = 4.6
        ),
        Place(
            name = "Ngũ Hành Sơn",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
            description = "Ngũ Hành Sơn gồm 5 ngọn núi đá cẩm thạch, nơi có nhiều hang động, chùa chiền và tượng Phật độc đáo.",
            address = "Hòa Hải, Ngũ Hành Sơn, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Di tích",
            rating = 4.5
        ),
        Place(
            name = "Công viên Châu Á",
            imageUrl = "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=800",
            description = "Công viên châu Á là khu vui chơi giải trí lớn nhất miền Trung với nhiều trò chơi cảm giác mạnh.",
            address = "Trường Sa, Ngũ Hành Sơn, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Công viên",
            rating = 4.3
        ),
        Place(
            name = "Cà phê Trứng Đà Nẵng",
            imageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800",
            description = "Quán cà phê view biển tuyệt đẹp tại Đà Nẵng, nổi tiếng với cà phê muối và cà phê trứng đặc sản.",
            address = "Võ Nguyên Giáp, Sơn Trà, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Cafe",
            rating = 4.4
        ),
        Place(
            name = "Bún chả cá Đà Nẵng",
            imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
            description = "Quán bún chả cá nổi tiếng nhất Đà Nẵng, món ăn đặc sản không thể bỏ qua khi đến thành phố biển.",
            address = "Lê Đình Dương, Hải Châu, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Quán ăn",
            rating = 4.6
        ),
        Place(
            name = "Bãi biển Non Nước",
            imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
            description = "Bãi biển Non Nước dài và yên tĩnh hơn Mỹ Khê, nước trong xanh, phù hợp để tắm biển và nghỉ dưỡng.",
            address = "Hòa Hải, Ngũ Hành Sơn, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Biển",
            rating = 4.5
        ),
        Place(
            name = "Bảo tàng Chăm",
            imageUrl = "https://images.unsplash.com/photo-1555400038-63f5ba517a47?w=800",
            description = "Bảo tàng Điêu khắc Chăm là bảo tàng lớn nhất thế giới về nghệ thuật điêu khắc Chăm Pa.",
            address = "2 Tháng 9, Hải Châu, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Di tích",
            rating = 4.4
        ),
        Place(
            name = "The Coffee House Đà Nẵng",
            imageUrl = "https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=800",
            description = "Chuỗi cà phê nổi tiếng với không gian đẹp, view nhìn ra biển Đà Nẵng, đồ uống đa dạng.",
            address = "Phạm Văn Đồng, Sơn Trà, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Cafe",
            rating = 4.3
        ),
        Place(
            name = "Chợ Hàn",
            imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
            description = "Chợ Hàn là khu chợ truyền thống lâu đời nhất Đà Nẵng, nơi bán đặc sản và quà lưu niệm.",
            address = "Bạch Đằng, Hải Châu, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Quán ăn",
            rating = 4.2
        ),
        Place(
            name = "Cầu Thuận Phước",
            imageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=800",
            description = "Cầu Thuận Phước là cầu treo dây võng dài nhất Việt Nam, view đẹp về phía biển và cảng Đà Nẵng.",
            address = "Thuận Phước, Hải Châu, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Check-in",
            rating = 4.3
        ),
        Place(
            name = "Công viên 29/3",
            imageUrl = "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=800",
            description = "Công viên 29/3 là lá phổi xanh lớn nhất Đà Nẵng với hồ nước, cây xanh và khu vui chơi trẻ em.",
            address = "Điện Biên Phủ, Thanh Khê, Đà Nẵng",
            city = "Đà Nẵng",
            category = "Công viên",
            rating = 4.1
        ),
        // ── HUẾ ──────────────────────────────────────────────────────────────
        Place(
            name = "Đại Nội Huế",
            imageUrl = "https://images.unsplash.com/photo-1555400038-63f5ba517a47?w=800",
            description = "Đại Nội Huế là kinh thành của triều Nguyễn, được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.",
            address = "26 Tháng 8, Thuận Hòa, Huế",
            city = "Huế",
            category = "Di tích",
            rating = 4.8
        ),
        Place(
            name = "Lăng Tự Đức",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
            description = "Lăng Tự Đức là công trình kiến trúc tiêu biểu của triều Nguyễn, được xây dựng trong 3 năm với kiến trúc thơ mộng.",
            address = "Kim Long, Huế",
            city = "Huế",
            category = "Di tích",
            rating = 4.7
        ),
        Place(
            name = "Chùa Thiên Mụ",
            imageUrl = "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800",
            description = "Chùa Thiên Mụ là ngôi chùa cổ nhất Huế, tháp Phước Duyên 7 tầng là biểu tượng của cố đô.",
            address = "Kim Long, Hương Long, Huế",
            city = "Huế",
            category = "Di tích",
            rating = 4.6
        ),
        Place(
            name = "Sông Hương",
            imageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800",
            description = "Sông Hương là con sông thơ mộng chảy qua trung tâm Huế, nơi du ngoạn thuyền rồng và nghe ca Huế.",
            address = "Trung tâm Huế",
            city = "Huế",
            category = "Check-in",
            rating = 4.5
        ),
        Place(
            name = "Cầu Tràng Tiền",
            imageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=800",
            description = "Cầu Tràng Tiền là cây cầu biểu tượng của Huế bắc qua sông Hương, đẹp nhất về đêm khi đèn sáng rực.",
            address = "Phú Hòa, Huế",
            city = "Huế",
            category = "Check-in",
            rating = 4.5
        ),
        Place(
            name = "Bún bò Huế O Liên",
            imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
            description = "Quán bún bò Huế nổi tiếng nhất cố đô, nước dùng đậm đà, thịt bò mềm, đặc trưng ẩm thực Huế.",
            address = "Nguyễn Công Trứ, Phú Hội, Huế",
            city = "Huế",
            category = "Quán ăn",
            rating = 4.7
        ),
        Place(
            name = "Cà phê Cung Đình",
            imageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800",
            description = "Cà phê phong cách cung đình Huế, không gian trang nhã với đồ nội thất cổ và nhạc dân tộc.",
            address = "Lê Lợi, Phú Hội, Huế",
            city = "Huế",
            category = "Cafe",
            rating = 4.4
        ),
        Place(
            name = "Công viên Phú Xuân",
            imageUrl = "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=800",
            description = "Công viên Phú Xuân nằm bên bờ sông Hương, không gian xanh mát, thoáng đãng giữa lòng thành phố.",
            address = "Lê Lợi, Phú Hội, Huế",
            city = "Huế",
            category = "Công viên",
            rating = 4.2
        ),
        Place(
            name = "Núi Ngự Bình",
            imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
            description = "Núi Ngự Bình là ngọn núi nhỏ phía nam Huế, từ đỉnh núi có thể nhìn toàn cảnh cố đô và sông Hương.",
            address = "Thủy An, Huế",
            city = "Huế",
            category = "Núi",
            rating = 4.3
        ),
        Place(
            name = "Lăng Khải Định",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
            description = "Lăng Khải Định nổi bật với kiến trúc kết hợp Đông Tây độc đáo, nội thất khảm sành sứ tinh xảo.",
            address = "Thủy Bằng, Hương Thủy, Huế",
            city = "Huế",
            category = "Di tích",
            rating = 4.6
        ),
        Place(
            name = "Bãi biển Thuận An",
            imageUrl = "https://images.unsplash.com/photo-1559628376-f3fe5f782a2e?w=800",
            description = "Bãi biển Thuận An cách trung tâm Huế 13km, bãi cát dài yên tĩnh, nước trong xanh.",
            address = "Thuận An, Phú Vang, Huế",
            city = "Huế",
            category = "Biển",
            rating = 4.3
        ),
        Place(
            name = "Chợ Đông Ba",
            imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
            description = "Chợ Đông Ba là chợ lớn nhất Huế, nơi bán đặc sản và ẩm thực đường phố Huế.",
            address = "Trần Hưng Đạo, Phú Hòa, Huế",
            city = "Huế",
            category = "Quán ăn",
            rating = 4.1
        )

    )

    // ─── Kiểm tra Firestore có writable không (có timeout) ───────────────────
    private suspend fun checkFirestoreWritable(): Boolean {
        return try {
            withTimeout(OP_TIMEOUT) {
                db.collection("_ping").document("test")
                    .set(mapOf("ts" to System.currentTimeMillis())).await()
                db.collection("_ping").document("test").delete().await()
                true
            }
        } catch (e: TimeoutCancellationException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    // ─── Tạo tài khoản admin ─────────────────────────────────────────────────
    private suspend fun createAdminAccount() {
        val adminEmail = "admin@travelmate.com"
        val adminPassword = "admin123456"
        try {
            withTimeout(OP_TIMEOUT) {
                val snapshot = db.collection("Users")
                    .whereEqualTo("email", adminEmail).get().await()
                if (!snapshot.isEmpty) return@withTimeout

                val result = auth.createUserWithEmailAndPassword(adminEmail, adminPassword).await()
                val uid = result.user?.uid ?: return@withTimeout
                val adminUser = User(
                    userId = uid,
                    fullName = "Admin TravelMate",
                    email = adminEmail,
                    avatarUrl = "https://ui-avatars.com/api/?name=Admin&background=4A90D9&color=fff&size=128",
                    role = "admin"
                )
                db.collection("Users").document(uid).set(adminUser).await()
            }
        } catch (e: Exception) { /* bỏ qua */ }
    }

    // ─── Seed địa điểm ───────────────────────────────────────────────────────
    private suspend fun seedPlaces(): Boolean {
        return try {
            withTimeout(OP_TIMEOUT) {
                val existing = db.collection("Places").get().await()
                if (!existing.isEmpty) return@withTimeout false

                val batch = db.batch()
                samplePlaces.forEach { place ->
                    val docRef = db.collection("Places").document()
                    batch.set(docRef, place.copy(placeId = docRef.id))
                }
                batch.commit().await()
                true
            }
        } catch (e: Exception) { false }
    }

    // ─── Seed reviews ─────────────────────────────────────────────────────────
    private suspend fun seedReviews(placeIds: List<String>) {
        if (placeIds.isEmpty()) return
        try {
            withTimeout(OP_TIMEOUT) {
                val existing = db.collection("Reviews").get().await()
                if (!existing.isEmpty) return@withTimeout

                val reviews = listOf(
                    Review(
                        userId = "sample_user_1", placeId = placeIds[0], rating = 5f,
                        comment = "Tuyệt vời! Cảnh đẹp không thể tả được.",
                        timestamp = System.currentTimeMillis() - 86400000L * 5,
                        authorName = "Nguyễn Văn An",
                        authorAvatar = "https://ui-avatars.com/api/?name=Nguyen+Van+An&background=4CAF50&color=fff&size=64"
                    ),
                    Review(
                        userId = "sample_user_2", placeId = placeIds[0], rating = 4f,
                        comment = "Rất đẹp, nên đi vào mùa thấp điểm.",
                        timestamp = System.currentTimeMillis() - 86400000L * 3,
                        authorName = "Trần Thị Bình",
                        authorAvatar = "https://ui-avatars.com/api/?name=Tran+Thi+Binh&background=E91E63&color=fff&size=64"
                    ),
                    Review(
                        userId = "sample_user_3",
                        placeId = if (placeIds.size > 1) placeIds[1] else placeIds[0],
                        rating = 5f,
                        comment = "Hội An về đêm đẹp như mơ!",
                        timestamp = System.currentTimeMillis() - 86400000L * 7,
                        authorName = "Lê Minh Châu",
                        authorAvatar = "https://ui-avatars.com/api/?name=Le+Minh+Chau&background=FF9800&color=fff&size=64"
                    )
                )
                val batch = db.batch()
                reviews.forEach { review ->
                    val docRef = db.collection("Reviews").document()
                    batch.set(docRef, review.copy(reviewId = docRef.id))
                }
                batch.commit().await()
            }
        } catch (e: Exception) { /* bỏ qua */ }
    }

    // ─── Seed timeline posts ──────────────────────────────────────────────────
    private suspend fun seedTimelinePosts() {
        try {
            withTimeout(OP_TIMEOUT) {
                val existing = db.collection("TravelPosts").get().await()
                if (!existing.isEmpty) return@withTimeout

                val posts = listOf(
                    TravelPost(
                        userId = "sample_user_1",
                        imageUrl = "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800",
                        caption = "Chuyến đi Hạ Long tuyệt vời! Biển xanh, trời trong.",
                        location = "Vịnh Hạ Long, Quảng Ninh",
                        createdAt = System.currentTimeMillis() - 86400000L * 2,
                        authorName = "Nguyễn Văn An",
                        authorAvatar = "https://ui-avatars.com/api/?name=Nguyen+Van+An&background=4CAF50&color=fff&size=64"
                    ),
                    TravelPost(
                        userId = "sample_user_2",
                        imageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800",
                        caption = "Hội An về đêm. Đèn lồng rực rỡ, phố cổ yên bình.",
                        location = "Phố cổ Hội An, Quảng Nam",
                        createdAt = System.currentTimeMillis() - 86400000L * 4,
                        authorName = "Trần Thị Bình",
                        authorAvatar = "https://ui-avatars.com/api/?name=Tran+Thi+Binh&background=E91E63&color=fff&size=64"
                    )
                )
                val batch = db.batch()
                posts.forEach { post ->
                    val docRef = db.collection("TravelPosts").document()
                    batch.set(docRef, post.copy(postId = docRef.id))
                }
                batch.commit().await()
            }
        } catch (e: Exception) { /* bỏ qua */ }
    }

    // ─── Chạy toàn bộ seed với timeout tổng 15 giây ──────────────────────────
    suspend fun runAll(): SeedResult {
        return try {
            withTimeout(15_000L) {
                // Bước 1: Kiểm tra Firestore writable
                val canWrite = checkFirestoreWritable()
                if (!canWrite) {
                    return@withTimeout SeedResult(
                        success = false,
                        needsRulesSetup = true,
                        error = "Firestore chưa cho phép ghi dữ liệu"
                    )
                }

                // Bước 2: Tạo admin
                createAdminAccount()

                // Bước 3: Seed places
                val placesSeeded = seedPlaces()

                // Bước 4: Seed reviews + posts (chỉ khi places mới được tạo)
                if (placesSeeded) {
                    val placeIds = try {
                        withTimeout(OP_TIMEOUT) {
                            db.collection("Places").get().await()
                                .documents.mapNotNull { it.id }
                        }
                    } catch (e: Exception) { emptyList() }

                    seedReviews(placeIds)
                    seedTimelinePosts()
                }

                SeedResult(
                    success = true,
                    placesSeeded = placesSeeded,
                    adminEmail = "admin@travelmate.com",
                    adminPassword = "admin123456"
                )
            }
        } catch (e: TimeoutCancellationException) {
            // Timeout toàn bộ — vẫn cho vào app bình thường
            SeedResult(
                success = true,
                placesSeeded = false,
                adminEmail = "admin@travelmate.com",
                adminPassword = "admin123456"
            )
        } catch (e: Exception) {
            SeedResult(success = false, error = e.message)
        }
    }
}

data class SeedResult(
    val success: Boolean = false,
    val placesSeeded: Boolean = false,
    val needsRulesSetup: Boolean = false,
    val adminEmail: String = "",
    val adminPassword: String = "",
    val error: String? = null
)
