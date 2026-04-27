package com.followup.presentation.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.google.firebase.auth.FirebaseAuth

class CambiarContrasenia : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cambiar_contrasenia)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        progressBar = findViewById(R.id.progressBar)
        tvEmail = findViewById(R.id.tvEmail)

        val userEmail = sharedPreferences.getString("USER_MAIL", "")
        tvEmail.text = userEmail

        findViewById<LinearLayout>(R.id.btnEnviarLink).setOnClickListener {
            sendPasswordResetLink()
        }
    }

    private fun sendPasswordResetLink() {
        val email = tvEmail.text.toString()

        if (email.isEmpty()) {
            Toast.makeText(this, R.string.error_envio_link, Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.link_enviado, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.error_envio_link, Toast.LENGTH_SHORT).show()
                }
            }
    }
}