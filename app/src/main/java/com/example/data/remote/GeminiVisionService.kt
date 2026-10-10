package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

sealed class GeminiOcrResult {
    data class Success(val titles: List<String>, val rawResponse: String) : GeminiOcrResult()
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val isTimeout: Boolean = false,
        val isKeyMissing: Boolean = false
    ) : GeminiOcrResult()
}

class GeminiVisionService {
    // Responsive timeouts for in-browser / container environments
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        // Automatic Resilient Fallback Chain: Primary gemini-2.5-flash -> gemini-2.5-flash-lite -> gemini-3.8-flash
        val MODEL_CHAIN = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.8-flash"
        )

        private const val OCR_PROMPT =
            "You are an expert movie/TV tracker. Analyze this screenshot and identify the main Movie, TV Show, or Anime title. Completely ignore status bars, clocks, battery icons, subtitles, and app UI text. Return ONLY a valid JSON object in this exact format: {\"title\": \"The Exact Title\"}. Do not wrap it in markdown code blocks or add any other text."
    }

    /**
     * Quickly verifies if a Gemini API key is valid by querying the models list.
     */
    suspend fun verifyApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty() || cleanKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext Result.failure(IllegalArgumentException("Key is empty or placeholder."))
        }

        try {
            val url = "$BASE_URL?key=$cleanKey"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Key verified successfully! (HTTP 200)")
                } else {
                    val code = response.code
                    val body = response.body?.string().orEmpty()
                    val errorMsg = parseErrorMessage(body, code)
                    Result.failure(Exception("Verification failed: $errorMsg"))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Network might be restricted in preview container."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Network unreachable (generativelanguage.googleapis.com not resolved)."))
        } catch (e: SSLException) {
            Result.failure(Exception("SSL/CORS handshake blocked by environment."))
        } catch (e: Exception) {
            Result.failure(Exception("Connection error: ${e.message}"))
        }
    }

    suspend fun extractTitles(
        apiKey: String,
        base64JpegData: String,
        onProgressUpdate: ((String) -> Unit)? = null
    ): GeminiOcrResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext GeminiOcrResult.Error(
                message = "Gemini API key is missing. Please set your key in Settings.",
                isKeyMissing = true
            )
        }

        val payload = buildJsonPayload(base64JpegData)

        var modelIndex = 0
        var retryCountForModel = 0
        var finalResult: GeminiOcrResult? = null

        while (modelIndex < MODEL_CHAIN.size && finalResult == null) {
            val currentModel = MODEL_CHAIN[modelIndex]
            onProgressUpdate?.invoke("Analyzing screenshot with $currentModel...")

            val url = "$BASE_URL/$currentModel:generateContent?key=$cleanKey"
            val requestBody = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val (responseCode, responseBody) = client.newCall(request).execute().use { response ->
                    Pair(response.code, response.body?.string().orEmpty())
                }

                // Handle 503 (High Demand / Service Unavailable) or 429 (Rate Limit)
                if (responseCode == 503 || responseCode == 429) {
                    if (retryCountForModel == 0) {
                        // 1.5-second automatic retry if the server is temporarily busy before failing
                        retryCountForModel++
                        onProgressUpdate?.invoke("Model $currentModel busy ($responseCode). Retrying in 1.5s...")
                        delay(1500)
                        continue
                    } else {
                        // Switch to next fallback model in chain
                        retryCountForModel = 0
                        modelIndex++
                        if (modelIndex < MODEL_CHAIN.size) {
                            val nextModel = MODEL_CHAIN[modelIndex]
                            onProgressUpdate?.invoke("High demand ($responseCode). Resiliently switching to $nextModel...")
                            delay(1000)
                            continue
                        } else {
                            finalResult = GeminiOcrResult.Error(
                                message = "All models experiencing high demand ($responseCode). Please use manual title fallback.",
                                isRateLimited = true
                            )
                            break
                        }
                    }
                }

                // Handle 404 (Model not found) -> automatically advance chain
                if (responseCode == 404) {
                    retryCountForModel = 0
                    modelIndex++
                    if (modelIndex < MODEL_CHAIN.size) {
                        val nextModel = MODEL_CHAIN[modelIndex]
                        onProgressUpdate?.invoke("Model $currentModel returned 404. Switching to $nextModel...")
                        continue
                    } else {
                        finalResult = GeminiOcrResult.Error("No available models found (404).")
                        break
                    }
                }

                // Handle other HTTP errors
                if (responseCode !in 200..299) {
                    val errMsg = parseErrorMessage(responseBody, responseCode)

                    // If error body indicates overloaded or high demand, advance model chain
                    if (responseBody.contains("high demand", ignoreCase = true) ||
                        responseBody.contains("overloaded", ignoreCase = true) ||
                        responseBody.contains("ResourceExhausted", ignoreCase = true)
                    ) {
                        retryCountForModel = 0
                        modelIndex++
                        if (modelIndex < MODEL_CHAIN.size) {
                            val nextModel = MODEL_CHAIN[modelIndex]
                            onProgressUpdate?.invoke("Model busy. Resiliently switching to $nextModel...")
                            delay(1500)
                            continue
                        }
                    }

                    finalResult = GeminiOcrResult.Error(errMsg)
                    break
                }

                // Parse Gemini candidate text
                val extractedText = parseCandidateText(responseBody)
                if (extractedText.isBlank()) {
                    finalResult = GeminiOcrResult.Error("No text was extracted from this screenshot.")
                    break
                }

                val titles = parseTitlesFromJsonText(extractedText)
                if (titles.isEmpty()) {
                    finalResult = GeminiOcrResult.Error("No movie/TV titles found in screenshot: \"$extractedText\"")
                    break
                }

                finalResult = GeminiOcrResult.Success(titles, responseBody)
                break
            } catch (e: SocketTimeoutException) {
                // If current model timed out, try next model in fallback chain
                modelIndex++
                if (modelIndex < MODEL_CHAIN.size) {
                    val nextModel = MODEL_CHAIN[modelIndex]
                    onProgressUpdate?.invoke("Timeout on $currentModel. Switching to $nextModel...")
                    continue
                } else {
                    finalResult = GeminiOcrResult.Error(
                        message = "Gemini request timed out. Container network may be constrained; please use manual title fallback.",
                        isTimeout = true
                    )
                    break
                }
            } catch (e: UnknownHostException) {
                finalResult = GeminiOcrResult.Error(
                    message = "Network host unreachable. Please verify connection or use manual title fallback.",
                    isTimeout = true
                )
                break
            } catch (e: SSLException) {
                finalResult = GeminiOcrResult.Error(
                    message = "SSL/CORS blocked in container. Please use manual title fallback.",
                    isTimeout = true
                )
                break
            } catch (e: ConnectException) {
                finalResult = GeminiOcrResult.Error(
                    message = "Could not connect to Gemini API endpoint.",
                    isTimeout = true
                )
                break
            } catch (e: IOException) {
                finalResult = GeminiOcrResult.Error("Network error: ${e.message ?: "Connection failure"}")
                break
            } catch (e: Exception) {
                finalResult = GeminiOcrResult.Error("Unexpected error: ${e.message}")
                break
            }
        }

        finalResult ?: GeminiOcrResult.Error("Extraction failed across model fallback chain.")
    }

    private fun buildJsonPayload(base64Data: String): String {
        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        val textPart = JSONObject()
        textPart.put("text", OCR_PROMPT)
        partsArray.put(textPart)

        val imagePart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Data)
        imagePart.put("inlineData", inlineData)
        partsArray.put(imagePart)

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        // Strict JSON output mode and low temperature for deterministic title extraction
        val generationConfig = JSONObject()
        generationConfig.put("responseMimeType", "application/json")
        generationConfig.put("temperature", 0.1)
        root.put("generationConfig", generationConfig)

        return root.toString()
    }

    private fun parseCandidateText(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text")
                if (text.isNotEmpty()) sb.append(text)
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseTitlesFromJsonText(rawText: String): List<String> {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 1. Primary: Parse strict JSON object {"title": "The Exact Title"}
        try {
            val jsonObj = JSONObject(cleaned)
            val title = jsonObj.optString("title")?.trim().orEmpty()
            if (title.isNotEmpty()) {
                return listOf(title)
            }
        } catch (_: Exception) {
            // Check for embedded JSON object { ... }
            val startBrace = cleaned.indexOf('{')
            val endBrace = cleaned.lastIndexOf('}')
            if (startBrace != -1 && endBrace > startBrace) {
                try {
                    val sub = cleaned.substring(startBrace, endBrace + 1)
                    val jsonObj = JSONObject(sub)
                    val title = jsonObj.optString("title")?.trim().orEmpty()
                    if (title.isNotEmpty()) {
                        return listOf(title)
                    }
                } catch (_: Exception) {
                    // Fall through
                }
            }
        }

        // 2. Strict regex targeting "title": "..."
        val strictTitleRegex = Regex("(?i)\"title\"\\s*:\\s*\"([^\"]+)\"")
        val strictMatch = strictTitleRegex.find(cleaned)
        if (strictMatch != null) {
            val title = strictMatch.groupValues[1].trim()
            if (title.isNotEmpty()) {
                return listOf(title)
            }
        }

        // 3. Fallback: Parse JSON array ["Title"] or [ { "title": "..." } ]
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(cleaned)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    val title = obj.optString("title")?.trim().orEmpty()
                    if (title.isNotEmpty() && !list.contains(title)) {
                        list.add(title)
                    }
                } else {
                    val item = jsonArray.optString(i)?.trim().orEmpty()
                    if (item.isNotEmpty() && !list.contains(item)) {
                        list.add(item)
                    }
                }
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {
            val startBracket = cleaned.indexOf('[')
            val endBracket = cleaned.lastIndexOf(']')
            if (startBracket != -1 && endBracket > startBracket) {
                try {
                    val sub = cleaned.substring(startBracket, endBracket + 1)
                    val subArray = JSONArray(sub)
                    for (i in 0 until subArray.length()) {
                        val item = subArray.optString(i)?.trim().orEmpty()
                        if (item.isNotEmpty() && !list.contains(item)) {
                            list.add(item)
                        }
                    }
                    if (list.isNotEmpty()) return list
                } catch (_: Exception) {
                    // Fall through
                }
            }
        }

        // 4. Fallback: Any quoted string
        val quoteRegex = Regex("\"([^\"]+)\"")
        val quoteMatches = quoteRegex.findAll(cleaned)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() && !it.equals("title", ignoreCase = true) }
            .toList()
        if (quoteMatches.isNotEmpty()) {
            return quoteMatches.distinct()
        }

        // 5. Final Fallback: Lines
        val lines = cleaned.lines().map {
            it.trim()
                .removePrefix("-")
                .removePrefix("*")
                .removePrefix("•")
                .trim()
        }.filter { it.isNotEmpty() && !it.startsWith("{") && !it.startsWith("[") && !it.startsWith("}") && !it.startsWith("]") }

        return lines.distinct()
    }

    private fun parseErrorMessage(responseBody: String, code: Int): String {
        return try {
            val root = JSONObject(responseBody)
            val errObj = root.optJSONObject("error")
            val message = errObj?.optString("message") ?: ""
            if (message.isNotEmpty()) {
                "Gemini Error ($code): $message"
            } else {
                "Gemini Error ($code)"
            }
        } catch (_: Exception) {
            "Gemini HTTP Error $code"
        }
    }
}
