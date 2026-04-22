package com.followup.presentation.register

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Usuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrarCuenta : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar_cuenta)

        firebaseAuth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tilName = findViewById<TextInputLayout>(R.id.til_Name)
        val tietName = findViewById<TextInputEditText>(R.id.tiet_Name)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_Email)
        val tietEmail = findViewById<TextInputEditText>(R.id.tiet_Email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_Password)
        val tietPassword = findViewById<TextInputEditText>(R.id.tiet_Password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_ConfirmPassword)
        val tietConfirmPassword = findViewById<TextInputEditText>(R.id.tiet_ConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_Register)
        val btnBack = findViewById<ImageButton>(R.id.btn_Back)

        btnBack.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val nombre = tietName.text.toString().trim()
            val email = tietEmail.text.toString().trim()
            val password = tietPassword.text.toString().trim()
            val confirmPassword = tietConfirmPassword.text.toString().trim()

            if (validarFrontend(nombre, email, password, confirmPassword, tilName, tilEmail, tilPassword, tilConfirmPassword)) {
                registrarUsuario(nombre, email, password)
            }
        }
    }

    private fun validarFrontend(
        nombre: String,
        email: String,
        password: String,
        confirmPassword: String,
        tilName: TextInputLayout,
        tilEmail: TextInputLayout,
        tilPassword: TextInputLayout,
        tilConfirmPassword: TextInputLayout
    ): Boolean {
        var esValido = true

        if (nombre.isEmpty()) {
            tilName.error = "El nombre es obligatorio"
            esValido = false
        } else tilName.error = null

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
        } else if (password.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            esValido = false
        } else tilPassword.error = null

        if (confirmPassword != password) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            esValido = false
        } else tilConfirmPassword.error = null

        return esValido
    }

    private fun registrarUsuario(nombre: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val dao = database.usuarioDao()
                            val existe = dao.obtenerPorMail(email)

                            if (existe == null) {
                                val nuevoUsuario = Usuario(
                                    nombre = nombre,
                                    mail = email,
                                    contraseniaHash = password,
                                    codigo2FA = null
                                )
                                dao.crearUsuario(nuevoUsuario)

                                // Guardar en SharedPreferences
                                val prefs = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putString("USER_NAME", nombre).apply()
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegistrarCuenta, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                        } catch (e: Exception) {
                            Log.e("REGISTRO_ERROR", "DB Error: ${e.message}")

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegistrarCuenta, "Error al guardar en base local", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    val errorMessage = task.exception?.message

                    when {
                        errorMessage?.contains("email already in use", true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo ya está en uso"
                        }
                        errorMessage?.contains("weak password", true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña muy débil"
                        }
                        else -> {
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}