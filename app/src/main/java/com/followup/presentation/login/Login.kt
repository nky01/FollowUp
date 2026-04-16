package com.followup.presentation.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.MainActivity
import com.followup.fragments.ReestablecerFragment
import com.followup.presentation.register.RegistrarCuenta
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tilEmail = findViewById<TextInputLayout>(R.id.til_Email)
        val tietEmail = findViewById<TextInputEditText>(R.id.tiet_Email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_Password)
        val tietPassword = findViewById<TextInputEditText>(R.id.tiet_Password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_Login)
        val tvRegister = findViewById<TextView>(R.id.tv_Register)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_ForgotPassword)

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegistrarCuenta::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = tietEmail.text.toString().trim()
            val password = tietPassword.text.toString().trim()

            if (validarFront(email, password, tilEmail, tilPassword)) {
                ejecutarLogin(email, password)
            }
        }

        tvForgotPassword.setOnClickListener {
            val email = tietEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilEmail.error = "Ingresá tu email primero"
                return@setOnClickListener
            }
            val intent = Intent(this, ReestablecerFragment::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
        }
    }

    private fun validarFront(email: String, password: String, tilEmail: TextInputLayout, tilPassword: TextInputLayout): Boolean {
        var esValido = true
        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email inválido"
            esValido = false
        } else {
            tilEmail.error = null
        }
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            esValido = false
        } else {
            tilPassword.error = null
        }
        return esValido
    }

    private fun ejecutarLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@Login)
                val dao = database.usuarioDao()

                val usuario = dao.obtenerPorMail(email)

                if (usuario != null) {
                    if (usuario.contraseniaHash == password) {
                        
                        // LIMPIAR PREFERENCIAS ANTERIORES PARA EVITAR CONFLICTOS
                        val oldPrefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        oldPrefs.edit().clear().apply()
                        val oldFollowUpPrefs = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
                        oldFollowUpPrefs.edit().clear().apply()

                        // GUARDAR NUEVOS DATOS
                        val prefs = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putInt("USER_ID", usuario.id)
                            putString("USER_NAME", usuario.nombre)
                            putString("USER_EMAIL", usuario.mail)
                            apply()
                        }
                        
                        val userDataPrefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
                        userDataPrefs.edit().apply {
                            putString("profile_image_uri", usuario.imagenPerfil)
                            apply()
                        }

                        Log.d("LOGIN_DEBUG", "Usuario ID: ${usuario.id}, Imagen: ${usuario.imagenPerfil}")

                        val intent = Intent(this@Login, com.followup.fragments.PrincipalActivity::class.java)
                        startActivity(intent)
                        finish() 
                    } else {
                        findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña incorrecta"
                    }
                } else {
                    findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo no está registrado"
                }
            } catch (e: Exception) {
                Log.e("LOGIN_ERROR", e.message ?: "Error desconocido")
                Toast.makeText(this@Login, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
