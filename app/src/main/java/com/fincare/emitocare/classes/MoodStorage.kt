package com.fincare.emitocare

import android.content.Context
import android.content.SharedPreferences

object MoodStorage {

    private const val PREF_NAME = "MoodPreferences"
    private const val KEY_MOOD = "user_mood"

    fun saveMood(context: Context, mood: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MOOD, mood).apply()
    }

    fun getMood(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MOOD, null)
    }

    fun clearMood(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MOOD).apply()
    }
}
