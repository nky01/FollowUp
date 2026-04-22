package com.followup.presentation.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.fragments.PrincipalActivity
import com.followup.fragments.ReestablecerFragment
import com.followup.presentation.register.RegistrarCuenta
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

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
            startActivity(Intent(this, RegistrarCuenta::class.java))
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

    private fun validarFront(
        email: String,
        password: String,
        tilEmail: TextInputLayout,
        tilPassword: TextInputLayout
    ): Boolean {
        var esValido = true

        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email inválido"
            esValido = false
        } else tilEmail.error = null

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            esValido = false
        } else tilPassword.error = null

        return esValido
    }

    private fun ejecutarLogin(email: String, password: String) {
        val emailLower = email.lowercase()

        firebaseAuth.signInWithEmailAndPassword(emailLower, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val dao = database.usuarioDao()
                            val usuario = dao.obtenerPorMail(emailLower)

                            withContext(Dispatchers.Main) {
                                sharedPreferences.edit {
                                    putString("USER_MAIL", emailLower)
                                    putString("USER_NAME", usuario?.nombre ?: "Usuario")
                                }

                                startActivity(Intent(this@Login, PrincipalActivity::class.java))
                                finish()
                            }

                        } catch (e: Exception) {
                            Log.e("LOGIN_ERROR", "DB error: ${e.message}")

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@Login, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    val errorMessage = task.exception?.message

                    when {
                        errorMessage?.contains("no user record", true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo no está registrado"
                        }
                        errorMessage?.contains("wrong password", true) == true ||
                                errorMessage?.contains("password", true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña incorrecta"
                        }
                        else -> {
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}
