package com.followup.presentation.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlin.random.Random

class CambiarMail : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var btnEnviarMail: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_mail)

        inicializarViews()
        inicializarSharedPreferences()
        inicializarDatabase()
        configurarClickListener()
    }

    private fun inicializarViews() {
        etEmail = findViewById(R.id.etEmail)
        btnEnviarMail = findViewById(R.id.btn_EnviarMail)
    }

    private fun inicializarSharedPreferences() {
        sharedPreferences = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
    }

    private fun inicializarDatabase() {
        database = AppDatabase.getDatabase(this)
    }

    private fun configurarClickListener() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, Configuracion::class.java))
            finish()
        }

        btnEnviarMail.setOnClickListener {
            validarYEnviarCodigo()
        }
    }

    private fun validarYEnviarCodigo() {
        val nuevoEmail = etEmail.text.toString().trim()

        if (!validarEmail(nuevoEmail)) {
            return
        }

        lifecycleScope.launch {
            try {
                val mailActual = obtenerMailActual()

                if (nuevoEmail.equals(mailActual, ignoreCase = true)) {
                    etEmail.error = "El nuevo email no puede ser igual al actual"
                    return@launch
                }

                val existeUsuario = database.usuarioDao().obtenerPorMail(nuevoEmail)
                if (existeUsuario != null) {
                    etEmail.error = "Este email ya está registrado"
                    return@launch
                }

                val codigo = generarCodigo6Digitos()
                enviarCodigoPorEmail(mailActual, codigo)

                etEmail.error = null
                Toast.makeText(this@CambiarMail, "Código enviado a $mailActual", Toast.LENGTH_LONG).show()

                navegarAVerificacion(mailActual, nuevoEmail, codigo)

            } catch (e: Exception) {
                Toast.makeText(this@CambiarMail, "Error al procesar la solicitud", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validarEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                etEmail.error = "El email es obligatorio"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Formato de email inválido"
                false
            }
            else -> {
                etEmail.error = null
                true
            }
        }
    }

    private fun obtenerMailActual(): String {
        return sharedPreferences.getString("USER_MAIL", "") ?: ""
    }

    private fun generarCodigo6Digitos(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private fun enviarCodigoPorEmail(destinatario: String, codigo: String) {
        // TODO: Implementar envío real de email mediante API o servicio de backend
        // Ejemplo de implementación futura:
        // val correo = Correo(destinatario, "Código de verificación: $codigo")
        // apiService.enviarCorreo(correo)

        // Por ahora, se guarda el código temporalmente para debug
        sharedPreferences.edit()
            .putString("CODIGO_VERIFICACION", codigo)
            .putString("NUEVO_EMAIL_TEMPORAL", etEmail.text.toString().trim())
            .apply()
    }

    private fun navegarAVerificacion(mailActual: String, nuevoEmail: String, codigo: String) {
        val intent = Intent(this, VerificarCambioMail::class.java)
        intent.putExtra("MAIL_ACTUAL", mailActual)
        intent.putExtra("NUEVO_EMAIL", nuevoEmail)
        intent.putExtra("CODIGO", codigo)
        startActivity(intent)
        finish()
    }
}
