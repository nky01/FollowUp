package com.followup.presentation.register

import android.os.Bundle
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
import kotlinx.coroutines.launch

class RegistrarCuenta : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar_cuenta)
        
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

        btnBack.setOnClickListener {
            finish()
        }

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
        nombre: String, email: String, password: String, confirmPassword: String,
        tilName: TextInputLayout, tilEmail: TextInputLayout, tilPassword: TextInputLayout, tilConfirmPassword: TextInputLayout
    ): Boolean {
        var esValido = true

        if (nombre.isEmpty()) {
            tilName.error = "El nombre es obligatorio"
            esValido = false
        } else {
            tilName.error = null
        }

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
        } else if (password.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            esValido = false
        } else {
            tilPassword.error = null
        }

        if (confirmPassword != password) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            esValido = false
        } else {
            tilConfirmPassword.error = null
        }

        return esValido
    }

    private fun registrarUsuario(nombre: String, email: String, password: String) {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@RegistrarCuenta)
                val dao = database.usuarioDao()

                val existe = dao.obtenerPorMail(email)
                if (existe == null) {
                    dao.crearUsuario(Usuario(nombre = nombre, mail = email, contraseniaHash = password, codigo2FA = null))
                    Toast.makeText(this@RegistrarCuenta, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo ya está en uso"
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegistrarCuenta, "Error al registrar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
