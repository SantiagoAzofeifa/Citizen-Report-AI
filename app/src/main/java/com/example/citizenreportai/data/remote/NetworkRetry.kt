package com.example.citizenreportai.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object NetworkRetry {
    const val DEFAULT_ATTEMPTS = 4
    const val DEFAULT_INITIAL_BACKOFF_MILLIS = 500L
    const val DEFAULT_MAX_BACKOFF_MILLIS = 4000L
    const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 20000L

    suspend fun <T> withRetry(
        attempts: Int = DEFAULT_ATTEMPTS,
        initialBackoffMillis: Long = DEFAULT_INITIAL_BACKOFF_MILLIS,
        maxBackoffMillis: Long = DEFAULT_MAX_BACKOFF_MILLIS,
        requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
        block: suspend () -> T
    ): T {
        var backoffMillis = initialBackoffMillis
        var lastError: Exception? = null

        for (attempt in 1..attempts) {
            try {
                return withTimeout(requestTimeoutMillis) { block() }
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) {
                    throw e
                }
                val httpException = e as? HttpException
                val statusCode = httpException?.code()
                val retryableIO = e is UnknownHostException ||
                    e is SocketTimeoutException ||
                    e is ConnectException
                val retryableTimeout = e is TimeoutCancellationException
                val shouldRetry = retryableTimeout || retryableIO || statusCode == 429 ||
                    (statusCode != null && statusCode >= 500)
                lastError = e
                if (!shouldRetry || attempt == attempts) break

                val retryAfterHeader = httpException?.response()?.headers()?.get("Retry-After")
                val retryAfterMillis = parseRetryAfter(retryAfterHeader, maxBackoffMillis)
                val delayMillis = retryAfterMillis?.coerceAtMost(maxBackoffMillis)
                    ?: backoffMillis.coerceAtMost(maxBackoffMillis)
                delay(delayMillis)
                if (retryAfterMillis == null) {
                    backoffMillis = (backoffMillis * 2).coerceAtMost(maxBackoffMillis)
                }
            }
        }
        throw lastError ?: IllegalStateException("Retry exhausted without error")
    }

    private fun parseRetryAfter(header: String?, maxBackoffMillis: Long): Long? {
        if (header.isNullOrBlank()) return null
        header.toLongOrNull()?.let { seconds ->
            return when {
                seconds <= 0 -> null
                seconds > maxBackoffMillis / 1000L -> maxBackoffMillis
                else -> seconds * 1000L
            }
        }
        return try {
            val retryAfterDate = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)
            val duration = Duration.between(Instant.now(), retryAfterDate.toInstant())
            if (duration.isNegative) null else duration.toMillis()
        } catch (_: Exception) {
            null
        }
    }
}
