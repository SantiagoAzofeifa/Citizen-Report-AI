package com.example.citizenreportai.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.citizenreportai.BuildConfig
import com.example.citizenreportai.data.model.ReportCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "ReportModerator"

// Modelo multimodal de Gemini en capa gratuita. Cámbialo si necesitas otro.
private const val MODEL = "gemini-2.5-flash"
private const val MAX_IMAGE_DIMEN = 1024   // px, lado mayor tras escalar
private const val JPEG_QUALITY = 80

/** Resultado de la validación de un reporte por la IA. */
sealed interface ModerationResult : java.io.Serializable {
    /**
     * El reporte es válido. [suggestedCategory] puede afinar la categoría elegida.
     * [imageDescription] es una frase breve de lo que la IA ve en la foto (null si no hay foto).
     */
    data class Approved(
        val suggestedCategory: ReportCategory?,
        val imageDescription: String? = null
    ) : ModerationResult

    /**
     * El reporte no es válido. [reason] es un texto corto para mostrar al usuario.
     * [imageDescription] es una frase breve de lo que la IA ve en la foto (null si no hay foto).
     */
    data class Rejected(
        val reason: String,
        val imageDescription: String? = null
    ) : ModerationResult

    /**
     * No se pudo evaluar (sin API key, sin red o error). El caller decide qué hacer;
     * por defecto dejamos pasar el reporte para no bloquear la app (fail-open).
     */
    data class Skipped(val reason: String) : ModerationResult
}

/**
 * Valida reportes ciudadanos con Gemini: comprueba que la foto y la descripción
 * sean apropiadas y tengan sentido como reporte urbano antes de enviarlos.
 *
 * La llamada se hace directamente desde la app (proyecto académico). En producción
 * esto debería vivir en el backend para no exponer la API key en el cliente.
 */
object ReportModerator {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()
    }

    private val categoryNames = ReportCategory.entries.joinToString(", ") { it.name }

    /**
     * Evalúa un reporte. Llamar desde una coroutine; el trabajo va a Dispatchers.IO.
     *
     * @param photoUri foto local del reporte (puede ser null si no hay foto).
     * @param description descripción escrita por el usuario (puede estar vacía).
     * @param category categoría seleccionada por el usuario.
     */
    suspend fun moderate(
        context: Context,
        photoUri: Uri?,
        description: String,
        category: ReportCategory
    ): ModerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY no configurada en local.properties; se omite la validación.")
            return@withContext ModerationResult.Skipped("Validación de IA no configurada")
        }

        // Nada que analizar: sin foto y sin descripción no llamamos a la IA.
        if (photoUri == null && description.isBlank()) {
            return@withContext ModerationResult.Skipped("Sin contenido que validar")
        }

        val imageBase64 = photoUri?.let { encodeImage(context, it) }
        if (photoUri != null && imageBase64 == null) {
            return@withContext ModerationResult.Skipped("No se pudo leer la imagen")
        }

        val requestJson = buildRequest(description, category, imageBase64)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini HTTP ${response.code}: $payload")
                    return@withContext ModerationResult.Skipped("Servicio de IA no disponible")
                }
                parseResponse(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción llamando a Gemini", e)
            ModerationResult.Skipped("No se pudo contactar al servicio de IA")
        }
    }

    private fun buildRequest(
        description: String,
        category: ReportCategory,
        imageBase64: String?
    ): JSONObject {
        val imageNote = if (imageBase64 != null) {
            "Se adjunta una imagen del reporte; analízala también."
        } else {
            "Este reporte NO incluye foto, así que valídalo solo por la descripción."
        }

        val prompt = """
            CONTEXTO
            Trabajas como moderador automático de "Citizen Report AI", una app donde la
            ciudadanía reporta problemas del espacio público a las autoridades locales para
            que los resuelvan. Un reporte legítimo describe un problema urbano/cívico real
            y accionable: baches, alumbrado público dañado, acumulación de basura, problemas
            de seguridad, daños en parques u otros desperfectos de la vía o el mobiliario
            público. Las autoridades destinan tiempo y recursos a cada reporte aceptado, por
            lo que tu filtro evita que pierdan tiempo con contenido inválido o abusivo.

            TU TAREA
            Decide si el reporte es VÁLIDO para publicarse y devuelve el JSON solicitado.
            Eres permisivo: tu único objetivo es filtrar contenido abusivo, sin sentido o
            ajeno al espacio público. NO juzgues la calidad de la redacción.

            RECHAZA (aprobado=false) SOLO si se cumple alguna de estas condiciones:
            - La descripción contiene insultos, groserías, amenazas, discurso de odio o
              ataques dirigidos a personas concretas.
            - La descripción es puro texto sin sentido o aleatorio (p. ej. "asdasd",
              "hsjkdhf", letras o teclas al azar que no forman ninguna idea).
            - La descripción es spam o publicidad evidente.
            - La imagen es inapropiada (desnudez, contenido sexual, violencia explícita,
              gore, odio).
            - La imagen o la descripción tratan de algo claramente ajeno al espacio público
              (p. ej. selfies, mascotas, comida, capturas de pantalla, memes).

            APRUEBA (aprobado=true) en todos los demás casos. En particular, SÍ son válidos:
            - Reportes cortos, vagos o escritos en primera persona (p. ej. "me asaltaron",
              "robaron en la esquina", "hay un bache", "no sirve la luz").
            - Incidentes de seguridad reales (asaltos, robos, vandalismo) → categoría SEGURIDAD.
            - Cualquier problema urbano/cívico comprensible, aunque le falten detalles.
            Puedes aprobarlo aunque la categoría elegida no sea la ideal; en ese caso indica
            la mejor en "categoriaSugerida".

            Ante la duda, APRUEBA. Solo rechaza cuando estés claramente seguro de que el
            contenido es abusivo, sin sentido o ajeno al espacio público.

            DATOS DEL REPORTE
            - $imageNote
            - Categoría elegida por el usuario: ${category.name}
            - Categorías posibles: $categoryNames
            - Descripción del usuario: "${description.ifBlank { "(vacía)" }}"

            Responde SOLO con el JSON pedido. El campo "motivo" debe ser una frase corta
            en español, clara y respetuosa para el ciudadano, explicando por qué se aprueba
            o rechaza (máx. 140 caracteres).

            En "descripcionImagen" describe en una sola frase breve y objetiva qué se ve en
            la foto (máx. 120 caracteres), en español. Si este reporte NO incluye foto, deja
            "descripcionImagen" como cadena vacía.
        """.trimIndent()

        val parts = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            if (imageBase64 != null) {
                put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", imageBase64)
                    )
                )
            }
        }

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("aprobado", JSONObject().put("type", "BOOLEAN"))
                    .put("motivo", JSONObject().put("type", "STRING"))
                    .put("descripcionImagen", JSONObject().put("type", "STRING"))
                    .put(
                        "categoriaSugerida",
                        JSONObject()
                            .put("type", "STRING")
                            .put("enum", JSONArray(ReportCategory.entries.map { it.name }))
                    )
            )
            .put("required", JSONArray().put("aprobado").put("motivo"))

        return JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", parts))
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema)
            )
    }

    private fun parseResponse(payload: String): ModerationResult {
        val text = runCatching {
            JSONObject(payload)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }.getOrNull()

        if (text == null) {
            Log.e(TAG, "Respuesta de Gemini sin texto: $payload")
            return ModerationResult.Skipped("Respuesta de IA no interpretable")
        }

        return runCatching {
            val result = JSONObject(text)
            val approved = result.getBoolean("aprobado")
            val reason = result.optString("motivo").ifBlank {
                "El reporte no cumple las normas de la comunidad."
            }
            val imageDescription = result.optString("descripcionImagen").takeIf { it.isNotBlank() }
            if (approved) {
                val suggested = result.optString("categoriaSugerida")
                    .takeIf { it.isNotBlank() }
                    ?.let { name -> ReportCategory.entries.firstOrNull { it.name == name } }
                ModerationResult.Approved(suggested, imageDescription)
            } else {
                ModerationResult.Rejected(reason, imageDescription)
            }
        }.getOrElse {
            Log.e(TAG, "No se pudo parsear el JSON del modelo: $text", it)
            ModerationResult.Skipped("Respuesta de IA no interpretable")
        }
    }

    /** Carga, escala y comprime la imagen a JPEG/base64 para enviarla inline a Gemini. */
    private fun encodeImage(context: Context, uri: Uri): String? = runCatching {
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val scaled = scaleDown(original, MAX_IMAGE_DIMEN)
        if (scaled !== original) original.recycle()

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        scaled.recycle()

        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }.getOrElse {
        Log.e(TAG, "No se pudo codificar la imagen", it)
        null
    }

    private fun scaleDown(bitmap: Bitmap, maxDimen: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimen) return bitmap
        val ratio = maxDimen.toFloat() / largest
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
