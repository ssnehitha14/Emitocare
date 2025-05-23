package com.fincare.emitocare

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ChatGPTService {
    private const val API_KEY = "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"
    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    fun getChatResponse(userMessage: String, context: Context, callback: (String, JSONObject) -> Unit) {
        val client = OkHttpClient()

        val jsonRequest = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content",
                    """
You are an empathetic AI assistant. Speak like a friendly human, not like an AI assistant.
You should not ask any questions, just have a conversation like a human. Replies should be short and simple.

Your tasks:
1. Detect the user's mood: [happy, sad, stressed, neutral].
2. Do NOT include any recommendation in the main message.
3. Do NOT include mood or JSON in the main message.
4.Based on the mood you should recommend and recommend those items which is easier o search on unsplash to get the images 
4. At the end of every reply, append a JSON like this:

{
  "recommendations": {
    "music": "text",
    "musicAudioUrl": "text",
    "story": "text",
    "travel": "text",
    "food": "text",
    "exercise": "text"
  }
}

Rules:
- For music:
    - Suggest a known free song.
    - For audio URL, ONLY use public free music URLs from https://pixabay.com/music/ (direct MP3 links).
    - Example format: "https://cdn.pixabay.com/audio/2023/04/05/audio_12345678.mp3"
- For food, suggest a popular dish except chicken and tomato.
- For travel, suggest a famous destination except bali.
- For exercise, suggest a common activity.
- For story, suggest a short story with explanation.

""".trimIndent()
                ))


                put(JSONObject().put("role", "user").put("content", userMessage))
            })
            put("temperature", 0.7)
        }

        val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}", JSONObject())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    try {
                        val jsonResponse = JSONObject(responseString)
                        if (jsonResponse.has("choices") && jsonResponse.getJSONArray("choices").length() > 0) {
                            val fullContent = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                            val splitIndex = fullContent.indexOf("{")
                            val messagePart = if (splitIndex > 0) fullContent.substring(0, splitIndex).trim() else fullContent
                            val jsonPart = if (splitIndex > 0) fullContent.substring(splitIndex).trim() else "{}"

                            val emotionRegex = "\\[Emotion: (.*?)\\]".toRegex()
                            val cleanedMessage = messagePart.replace(emotionRegex, "").trim()

                            val recommendations = try {
                                val obj = JSONObject(jsonPart)
                                obj.getJSONObject("recommendations")
                            } catch (e: Exception) {
                                JSONObject()
                            }

                            callback(cleanedMessage, recommendations)

                            ChatSessionStorage.saveMessage(context, userMessage, "user")
                            ChatSessionStorage.saveMessage(context, cleanedMessage, "bot")
                        } else if (jsonResponse.has("error")) {
                            val errorMessage = jsonResponse.getJSONObject("error").getString("message")
                            callback("API Error: $errorMessage", JSONObject())
                        } else {
                            callback("Unexpected response from API", JSONObject())
                        }
                    } catch (e: Exception) {
                        callback("JSON Parsing Error: ${e.message}", JSONObject())
                    }
                } ?: callback("Error: Empty response from API", JSONObject())
            }
        })
    }
}
