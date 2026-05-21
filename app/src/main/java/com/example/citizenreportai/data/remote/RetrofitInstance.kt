package com.example.citizenreportai.data.remote

import com.example.citizenreportai.BuildConfig
import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

object RetrofitInstance {
    private val BASE_URL = BuildConfig.API_BASE_URL

    private fun getIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val dateAdapter = object : JsonDeserializer<Date>, JsonSerializer<Date> {
        override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): Date {
            return try {
                val raw = json.asString
                getIsoFormat().parse(raw) ?: Date()
            } catch (e: Exception) {
                try {
                    val timestamp = json.asLong
                    Date(if (timestamp < 100_000_000_000L) timestamp * 1000 else timestamp)
                } catch (ex: Exception) {
                    Date()
                }
            }
        }

        override fun serialize(src: Date, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(getIsoFormat().format(src))
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, dateAdapter)
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
