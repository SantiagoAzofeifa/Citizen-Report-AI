// Edita C:/Users/santi/OneDrive/Documentos/UNA/Moviles/Citizen-Report-AI/app/src/main/java/com/example/citizenreportai/data/remote/RetrofitInstance.kt

package com.example.citizenreportai.data.remote

import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.Date

object RetrofitInstance {
    private const val BASE_URL = "https://69d6f5779c5ebb0918c6d9a2.mockapi.io/"

    // Adaptador personalizado para manejar fechas en segundos (Long) o String (ISO)
    private val dateDeserializer = JsonDeserializer { json, _, _ ->
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            Date(json.asLong * 1000) // MockAPI suele enviar segundos, Java necesita milisegundos
        } else {
            // Intentar parsear como string ISO
            try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                format.parse(json.asString)
            } catch (e: Exception) {
                Date() // Fallback a fecha actual en caso de error
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, dateDeserializer)
        .create()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}