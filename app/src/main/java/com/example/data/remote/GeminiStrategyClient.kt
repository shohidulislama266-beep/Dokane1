package com.example.data.remote

import com.example.data.model.BusinessStrategyFacts
import com.example.data.model.SavedBusinessStrategy
import com.example.domain.BusinessStrategyBanglaBuilder
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

interface StrategyAiService {
    suspend fun testApiKey(apiKey: String): Result<Unit>
    suspend fun generateBanglaStrategy(
        apiKey: String,
        storeId: String,
        facts: BusinessStrategyFacts,
        generatedAtMs: Long,
        refreshCountToday: Int,
        updatedBy: String
    ): Result<SavedBusinessStrategy>
}

/**
 * Calls Gemini REST API with an owner-supplied API key sent via x-goog-api-key header.
 * Security: Never logs the API key or HTTP request/response headers.
 */
class GeminiStrategyClient : StrategyAiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("API key is too short"))
        }
        try {
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", "OK")))
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("maxOutputTokens", 8)
                        put("temperature", 0.0)
                    }
                )
            }
            val request = Request.Builder()
                .url(GENERATE_ENDPOINT)
                .addHeader("x-goog-api-key", cleanKey)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Gemini HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            // Do not include URL or headers or any exception details that could echo secrets
            Result.failure(IOException("Gemini connection failed"))
        }
    }

    override suspend fun generateBanglaStrategy(
        apiKey: String,
        storeId: String,
        facts: BusinessStrategyFacts,
        generatedAtMs: Long,
        refreshCountToday: Int,
        updatedBy: String
    ): Result<SavedBusinessStrategy> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Missing valid Gemini API key"))
        }
        try {
            val factSheet = BusinessStrategyBanglaBuilder.buildPromptFactText(facts)
            val fullPrompt = "${BusinessStrategyBanglaBuilder.buildGeminiSystemPrompt()}\n\nদোকানের আজকের তথ্য:\n$factSheet"
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.2)
                        put("maxOutputTokens", 500)
                        put("responseMimeType", "application/json")
                    }
                )
            }

            val request = Request.Builder()
                .url(GENERATE_ENDPOINT)
                .addHeader("x-goog-api-key", cleanKey)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Gemini HTTP ${response.code}"))
                }
                val rawBody = response.body?.string().orEmpty()
                val text = extractCandidateText(rawBody)
                    ?: return@withContext Result.failure(IOException("Empty Gemini response"))
                val parsed = BusinessStrategyBanglaBuilder.parseAiJsonToStrategy(
                    rawJsonText = text,
                    storeId = storeId,
                    facts = facts,
                    generatedAtMs = generatedAtMs,
                    refreshCountToday = refreshCountToday,
                    updatedBy = updatedBy
                )
                Result.success(parsed)
            }
        } catch (_: Exception) {
            Result.failure(IOException("Gemini request failed"))
        }
    }

    private fun extractCandidateText(rawResponseJson: String): String? {
        return try {
            val root = JSONObject(rawResponseJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.optJSONObject(0)?.optString("text", "")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val GENERATE_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }
}
