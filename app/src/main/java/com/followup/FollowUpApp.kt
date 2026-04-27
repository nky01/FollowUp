package com.followup

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class FollowUpApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}