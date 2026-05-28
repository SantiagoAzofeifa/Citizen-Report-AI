package com.example.citizenreportai.data.remote

import com.example.citizenreportai.BuildConfig
import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val BASE_URL = BuildConfig.API_BASE_URL

    private fun getIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private fun parseIsoLenient(raw: String): Date? {
        // Backend may return microseconds (".449974Z") or no fraction at all.
        // Normalize to milliseconds (3 fractional digits) before parsing.
        val normalized = run {
            val dotIdx = raw.indexOf('.')
            if (dotIdx < 0) {
                raw.removeSuffix("Z").let { "$it.000Z" }
            } else {
                val zIdx = raw.indexOf('Z', dotIdx)
                val end = if (zIdx >= 0) zIdx else raw.length
                val fraction = raw.substring(dotIdx + 1, end).padEnd(3, '0').take(3)
                raw.substring(0, dotIdx) + "." + fraction + "Z"
            }
        }
        return try { getIsoFormat().parse(normalized) } catch (_: Exception) { null }
    }

    private val dateAdapter = object : JsonDeserializer<Date>, JsonSerializer<Date> {
        override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): Date {
            return try {
                parseIsoLenient(json.asString) ?: Date()
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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
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
