package com.fincare.emitocare

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

object UnsplashService {
    private const val ACCESS_KEY = "jEsEZhR2BkCfizdWnY-RNlDZgOIAi_BY8lEyqiz6CV4"
    private const val UNSPLASH_URL = "https://api.unsplash.com/search/photos"

    /**
     * Fetches a single image URL for the given prompt.
     * Tries up to 50 results and picks the first whose alt_description
     * contains all words in the prompt. Falls back to result[0].
     */
    fun fetchImageUrl(prompt: String, callback: (String) -> Unit) {
        val client = OkHttpClient()

        // Wrap prompt in quotes to search as a phrase
        val quotedPrompt = "\"$prompt\""

        val url = UNSPLASH_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("query",quotedPrompt) // This ensures phrases like "chicken soup" are preserved
            ?.addQueryParameter("client_id", ACCESS_KEY)
            ?.addQueryParameter("orientation", "squarish")
            ?.addQueryParameter("per_page", "5")
            ?.build()

        Log.d("UnsplashService", "Request URL: $url")

        val request = Request.Builder()
            .url(url!!)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UnsplashService", "Error fetching images", e)
                callback("") // Return empty on error
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    callback("")
                    return
                }

                try {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results == null || results.length() == 0) {
                        callback("")
                        return
                    }

                    val words = prompt
                        .split("\\s+".toRegex())
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }

                    var selectedUrl: String? = null
                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val desc = obj.optString("alt_description")?.lowercase() ?: ""
                        if (words.all { desc.contains(it) }) {
                            selectedUrl = obj.getJSONObject("urls").optString("regular")
                            break
                        }
                    }

                    // Fallback to first image if no match found
                    if (selectedUrl == null) {
                        selectedUrl = results.getJSONObject(0)
                            .getJSONObject("urls")
                            .optString("regular")
                    }

                    callback(selectedUrl ?: "")

                } catch (e: Exception) {
                    Log.e("UnsplashService", "Error parsing JSON", e)
                    callback("")
                }
            }
        })
    }
}
