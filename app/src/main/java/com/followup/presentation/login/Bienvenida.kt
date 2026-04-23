package com.followup.presentation.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.followup.fragments.PrincipalActivity
import com.google.firebase.auth.FirebaseAuth

class Bienvenida : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida)
        
        firebaseAuth = FirebaseAuth.getInstance()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkUserSession()
    }

    private fun checkUserSession() {
        val currentUser = firebaseAuth.currentUser
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                val intent = Intent(this, PrincipalActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, Login::class.java)
                startActivity(intent)
            }
            finish()
        }, 1500)
    }
}