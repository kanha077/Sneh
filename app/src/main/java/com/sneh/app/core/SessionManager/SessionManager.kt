package com.sneh.app.core

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("sneh_prefs", Context.MODE_PRIVATE)

    fun setLoggedIn(value: Boolean) {
        prefs.edit().putBoolean("is_logged_in", value).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun setOnboarded(value: Boolean) {
        prefs.edit().putBoolean("is_onboarded", value).apply()
    }

    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)

    fun logout() {
        prefs.edit().clear().apply()
    }
}