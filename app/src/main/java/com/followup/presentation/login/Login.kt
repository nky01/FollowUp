package com.followup.presentation.login

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
        // lifecycleScope para ejecutar la consulta Db y evitar bloquear la UI,
        // por ejemplo: si la DB tarda en responder o hay un error de conexión, no se congela la app y se muestra un Toast de error
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@Login)
                val dao = database.usuarioDao()
                
                // Buscar usuario en la DB
                val usuario = dao.obtenerPorMail(email)
                
                if (usuario != null) {
                    // Verificar contraseña
                    if (usuario.contraseniaHash == password) {
                        // Ir al Home
                        val intent = Intent(this@Login, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Cerrar el login para que no se pueda volver atrás
                    } else {
                        findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña incorrecta"
                    }
                } else {
                    findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo no está registrado"
                }
                // toast es una notificación breve que aparece en la pantalla
            } catch (e: Exception) {
                Toast.makeText(this@Login, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
