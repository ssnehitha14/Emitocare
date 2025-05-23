package com.fincare.emitocare

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object ChatSessionStorage {
    private const val PREFS_NAME = "chat_session_prefs"
    private const val CHAT_HISTORY_KEY = "chat_history"

    fun saveMessage(context: Context, message: String, sender: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val chatHistory = getChatHistory(context)
        val messageObject = JSONObject()
        messageObject.put("message", message)
        messageObject.put("sender", sender)
        messageObject.put("timestamp", System.currentTimeMillis())

        chatHistory.put(messageObject)
        editor.putString(CHAT_HISTORY_KEY, chatHistory.toString())
        editor.apply()
    }

    fun getChatHistory(context: Context): JSONArray {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val chatHistoryString = sharedPreferences.getString(CHAT_HISTORY_KEY, "[]")

        return try {
            JSONArray(chatHistoryString)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun clearChatHistory(context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(CHAT_HISTORY_KEY)
        editor.apply()
    }
}
