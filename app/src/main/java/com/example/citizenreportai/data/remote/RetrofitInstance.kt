package com.example.citizenreportai.data.remote

import com.example.citizenreportai.BuildConfig
import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

object RetrofitInstance {
    private val BASE_URL = BuildConfig.API_BASE_URL

    // Acepta epoch en segundos/milisegundos y fechas ISO (con/sin ms, con offset)
    private val dateDeserializer = JsonDeserializer<Date> { json, _, _ ->
        val primitive = json.asJsonPrimitive
        if (primitive.isNumber) {
            val raw = primitive.asLong
            val millis = if (raw < 100_000_000_000L) raw * 1000 else raw
            Date(millis)
        } else {
            parseDate(primitive.asString) ?: Date()
        }
    }

    private fun parseDate(raw: String): Date? {
        return runCatching { Date.from(Instant.parse(raw)) }
            .recoverCatching { Date.from(OffsetDateTime.parse(raw).toInstant()) }
            .recoverCatching { Date.from(LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)) }
            .getOrNull()
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