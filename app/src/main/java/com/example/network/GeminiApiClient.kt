package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- API Request and Response Models using Moshi ---

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Bas64 encoded string
)

data class Content(
    val parts: List<Part>
)

data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Double? = 0.1
)

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = GenerationConfig()
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ResponseContent?
)

data class ResponseContent(
    val parts: List<ResponsePart>?
)

data class ResponsePart(
    val text: String?
)

// --- Local Extraction Result Model ---
data class ExtractedExpense(
    val totalAmount: Double,
    val billName: String,
    val vendorName: String,
    val date: String, // YYYY-MM-DD
    val category: String // "Food", "Utility", "Subscriptions", "Others"
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun extractExpenseFromImage(base64Image: String, mimeType: String = "image/jpeg"): ExtractedExpense? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured in Secrets panel")
        }

        val prompt = """
            You are an expert AI receipt OCR scanner.
            Analyze this invoice/receipt image and extract:
            1. Total amount (Double: extract decimal digits only, if currency symbol is present, ignore the symbol itself).
            2. Invoice/Bill name (String: brief descriptor of what was purchased).
            3. Vendor/Merchant name (String: store name, company name).
            4. Date (String: Format as YYYY-MM-DD. If year is missing, assume current year 2026).
            5. Category (String: Classify strictly as one of: "Food", "Utility", "Subscriptions", "Others"). Use rules:
               - "Food": restaurants, grocery, Starbucks, UberEats, McDonald's.
               - "Utility": power, electricity, water, internet, trash, Comcast, AT&T.
               - "Subscriptions": Spotify, Netflix, AWS, OpenAI, GitHub, SaaS.
               - "Others": any other retail, department store, clothes, unclassified.

            Return strictly a valid JSON object matching this schema exactly, do not output any surrounding text or markdown formatting:
            {
              "totalAmount": 0.0,
              "billName": "",
              "vendorName": "",
              "date": "YYYY-MM-DD",
              "category": ""
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig()
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse ExtractedExpense from JSON string
                val cleanJson = cleanJsonString(jsonText)
                val adapter = moshi.adapter(ExtractedExpense::class.java)
                adapter.fromJson(cleanJson)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateFinancialInsights(expenseDataJson: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured in Secrets panel")
        }

        val prompt = """
            You are a senior financial advisor and assistant.
            Review the following list of active expenses and generate bulletproof AI-powered spending insights:
            - General overspending trends or high spending periods.
            - Identify categories that occupy too much budget.
            - Provide clear actionable budgeting recommendations.
            - Highlight recurring subscriptions if detected.

            Keep the tone clean, analytical, professional, and strictly action-oriented.
            DO NOT include any emojis anywhere.
            Format the response as clear bullet points.

            Expense Log Data:
            $expenseDataJson
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(responseMimeType = "text/plain")
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanJsonString(rawJson: String): String {
        var clean = rawJson.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
