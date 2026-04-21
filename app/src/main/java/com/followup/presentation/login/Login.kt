package com.followup.presentation.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

        // Ir a la pantalla de Registro
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegistrarCuenta::class.java)
            startActivity(intent)
        }

        // Lógica de Inicio de Sesión
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

        // Validar Email
        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email inválido"
            esValido = false
        } else {
            tilEmail.error = null
        }

        // Validar Password
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            esValido = false
        } else {
            tilPassword.error = null
        }

        return esValido
    }

    private fun ejecutarLogin(email: String, password: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val dao = database.usuarioDao()

                val usuario = dao.obtenerPorMail(email)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (usuario != null) {
                        if (usuario.contraseniaHash == password) {

                            // GUARDAR NOMBRE DEL USUARIO PARA USAR EN TODA LA APP
                            val sharedPreferences = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit()
                                .putString("USER_NAME", usuario.nombre)
                                .apply()

                            val intent = Intent(this@Login, com.followup.fragments.PrincipalActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña incorrecta"
                        }
                    } else {
                        findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo no está registrado"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LOGIN_ERROR", "Error: ${e.message}")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(this@Login, "Error de DB: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
