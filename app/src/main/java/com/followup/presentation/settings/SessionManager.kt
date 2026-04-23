package com.followup.presentation.settings

import android.content.Context

/*  IMPORTANTE PARA QUE CADA USUARIO TENGA CLIENTES Y VENTAS DISTINTAS... */

class SessionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)

    fun getUserMail(): String {
        return prefs.getString("USER_MAIL", "") ?: ""
    }

    fun saveUserMail(email: String) {
        prefs.edit().putString("USER_MAIL", email).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

}