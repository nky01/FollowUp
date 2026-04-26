package com.followup.presentation.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.followup.presentation.login.Bienvenida
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit

class Configuracion : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        findViewById<TextView>(R.id.tvUserName).text = userName

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<LinearLayout>(R.id.ll_CambiarMail).setOnClickListener {
            startActivity(Intent(this, CambiarMail::class.java))
        }

        findViewById<LinearLayout>(R.id.llSeguridad).setOnClickListener {
            startActivity(Intent(this, Seguridad::class.java))
        }

        findViewById<LinearLayout>(R.id.ll_CerrarSesion).setOnClickListener {
            firebaseAuth.signOut()
            sharedPreferences.edit { clear() }
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Bienvenida::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.ll_Apariencia).setOnClickListener {
            mostrarDialogApariencia()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun mostrarDialogApariencia() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_apariencia, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val switchDark = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_dark_mode)

        // Cargar preferencia guardada
        val isDark = sharedPreferences.getBoolean("dark_mode", false)
        switchDark.isChecked = isDark

        switchDark.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cerrar_apariencia)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}