package com.example.travelmate.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryUploader {

    private const val CLOUD_NAME = "dpslikjgu"
    private const val UPLOAD_PRESET = "travelmate_upload"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    /**
     * Upload ảnh lên Cloudinary từ Uri (ảnh chọn từ điện thoại).
     * Trả về URL ảnh đã upload.
     */
    suspend fun uploadImage(context: Context, uri: Uri, folder: String = "travelmate"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Không thể đọc ảnh"))

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                val boundary = "----FormBoundary${System.currentTimeMillis()}"
                val url = URL(UPLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    connectTimeout = 30_000
                    readTimeout = 30_000
                }

                val outputStream = DataOutputStream(connection.outputStream)

                // Field: upload_preset
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                outputStream.writeBytes("$UPLOAD_PRESET\r\n")

                // Field: folder
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
                outputStream.writeBytes("$folder\r\n")

                // Field: file (ảnh)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                outputStream.writeBytes("Content-Type: $mimeType\r\n\r\n")
                outputStream.write(imageBytes)
                outputStream.writeBytes("\r\n")

                // End boundary
                outputStream.writeBytes("--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val secureUrl = json.getString("secure_url")
                    Result.success(secureUrl)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText() ?: "Upload thất bại"
                    Result.failure(Exception("Lỗi $responseCode: $error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
