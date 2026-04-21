package com.followup.presentation.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R

class Configuracion : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        findViewById<TextView>(R.id.tvUserName).text = userName

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.ll_CambiarMail).setOnClickListener {
            startActivity(Intent(this, CambiarMail::class.java))
        }

        findViewById<LinearLayout>(R.id.llSeguridad).setOnClickListener {
            startActivity(Intent(this, Seguridad::class.java))
        }
    }
}