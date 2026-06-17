# Firebase Setup Guide – TravelMate

## 1. Firestore Security Rules

Vào Firebase Console → Firestore → Rules → Paste nội dung sau:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users: chỉ đọc được thông tin của mình, admin đọc tất cả
    match /Users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Places: mọi người đọc được, chỉ admin mới ghi
    match /Places/{placeId} {
      allow read: if true;
      allow write: if request.auth != null &&
        get(/databases/$(database)/documents/Users/$(request.auth.uid)).data.role == 'admin';
    }

    // Favorites: user chỉ đọc/ghi của mình
    match /Favorites/{favId} {
      allow read, write: if request.auth != null &&
        (resource == null || resource.data.userId == request.auth.uid);
      allow create: if request.auth != null &&
        request.resource.data.userId == request.auth.uid;
    }

    // Trips: user chỉ đọc/ghi của mình
    match /Trips/{tripId} {
      allow read, write: if request.auth != null &&
        (resource == null || resource.data.userId == request.auth.uid);
      allow create: if request.auth != null &&
        request.resource.data.userId == request.auth.uid;
    }

    // TripPlaces: user đã đăng nhập
    match /TripPlaces/{id} {
      allow read, write: if request.auth != null;
    }

    // TravelPosts: mọi người đọc, user ghi của mình, admin xóa tất cả
    match /TravelPosts/{postId} {
      allow read: if true;
      allow create: if request.auth != null &&
        request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null &&
        (resource.data.userId == request.auth.uid ||
         get(/databases/$(database)/documents/Users/$(request.auth.uid)).data.role == 'admin');
    }

    // Reviews: mọi người đọc, user ghi của mình, admin xóa tất cả
    match /Reviews/{reviewId} {
      allow read: if true;
      allow create: if request.auth != null &&
        request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null &&
        (resource.data.userId == request.auth.uid ||
         get(/databases/$(database)/documents/Users/$(request.auth.uid)).data.role == 'admin');
      allow update: if request.auth != null;
    }
  }
}
```

## 2. Firebase Storage Rules

Vào Firebase Console → Storage → Rules:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null
        && request.resource.size < 10 * 1024 * 1024  // max 10MB
        && request.resource.contentType.matches('image/.*');
    }
  }
}
```

## 3. Firebase Authentication

Vào Firebase Console → Authentication → Sign-in method:
- Bật **Email/Password**

## 4. Tài khoản Admin mặc định

Sau khi chạy app lần đầu, tài khoản admin sẽ được tạo tự động:
- **Email:** admin@travelmate.com
- **Mật khẩu:** admin123456

## 5. Dữ liệu mẫu

App sẽ tự động seed 10 địa điểm, 4 bài timeline và 4 review mẫu khi chạy lần đầu.
