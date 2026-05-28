package com.example.citizenreportai.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.citizenreportai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoUploader"

object PhotoUploader {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Sube un archivo local a Cloudinary (unsigned). Devuelve la URL https pública,
     * o null si falla. Llamar desde una coroutine; el trabajo va a Dispatchers.IO.
     */
    suspend fun upload(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        if (cloudName.startsWith("REEMPLAZAR") || preset.startsWith("REEMPLAZAR")) {
            Log.e(TAG, "Credenciales de Cloudinary no configuradas en build.gradle.kts")
            return@withContext null
        }
        Log.d(TAG, "Subiendo a cloud=$cloudName preset=$preset uri=$uri")

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrElse {
            Log.e(TAG, "No se pudo leer el archivo local", it)
            null
        } ?: return@withContext null
        Log.d(TAG, "Tamaño del archivo: ${bytes.size} bytes")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart(
                name = "file",
                filename = "report.jpg",
                body = bytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Cloudinary HTTP ${response.code}: $payload")
                    return@withContext null
                }
                val url = JSONObject(payload).optString("secure_url").takeIf { it.isNotBlank() }
                if (url == null) Log.e(TAG, "Respuesta sin secure_url: $payload")
                else Log.d(TAG, "Subido OK: $url")
                url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción subiendo a Cloudinary", e)
            null
        }
    }
}
