package com.followup.presentation.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.followup.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class CambiarMail : AppCompatActivity() {

    private lateinit var tvInstruccion: TextView
    private lateinit var tvError: TextView
    private lateinit var tvMensajeExito: TextView
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnVerificar: MaterialButton
    private lateinit var btnEnviarMail: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth

    private var isReauthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_mail)

        inicializarViews()
        inicializarSharedPreferences()
        inicializarFirebaseAuth()
        configurarClickListener()
    }

    private fun inicializarViews() {
        tvInstruccion = findViewById(R.id.tvInstruccion)
        tvError = findViewById(R.id.tvError)
        tvMensajeExito = findViewById(R.id.tvMensajeExito)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnVerificar = findViewById(R.id.btn_Verificar)
        btnEnviarMail = findViewById(R.id.btn_EnviarMail)
    }

    private fun inicializarSharedPreferences() {
        sharedPreferences = getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
    }

    private fun inicializarFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun configurarClickListener() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        btnVerificar.setOnClickListener {
            if (!isReauthenticated) {
                verificarPassword()
            }
        }

        btnEnviarMail.setOnClickListener {
            validarYEnviarVerificacion()
        }
    }

    private fun verificarPassword() {
        val password = etPassword.text.toString()

        if (password.isEmpty()) {
            tvError.text = "La contraseña es obligatoria"
            tvError.visibility = View.VISIBLE
            return
        }

        tvError.visibility = View.GONE
        btnVerificar.isEnabled = false
        btnVerificar.text = "Verificando..."

        val currentUser = firebaseAuth.currentUser
        val currentEmail = currentUser?.email

        if (currentUser == null || currentEmail == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            btnVerificar.isEnabled = true
            btnVerificar.text = "Confirmar"
            return
        }

        val credential = EmailAuthProvider.getCredential(currentEmail, password)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                isReauthenticated = true
                mostrarFormularioNuevoMail()
            }
            .addOnFailureListener { e ->
                tvError.text = "Contraseña incorrecta"
                tvError.visibility = View.VISIBLE
                btnVerificar.isEnabled = true
                btnVerificar.text = "Confirmar"
            }
    }

    private fun mostrarFormularioNuevoMail() {
        tvInstruccion.text = "Ingresá el nuevo correo electrónico que usarás para iniciar sesión."
        tilPassword.visibility = View.GONE
        btnVerificar.visibility = View.GONE
        tvError.visibility = View.GONE
        tilEmail.visibility = View.VISIBLE
        btnEnviarMail.visibility = View.VISIBLE
        etEmail.requestFocus()
    }

    private fun validarYEnviarVerificacion() {
        val nuevoEmail = etEmail.text.toString().trim().lowercase()

        if (!validarEmail(nuevoEmail)) {
            return
        }

        val mailActual = firebaseAuth.currentUser?.email

        if (nuevoEmail.equals(mailActual, ignoreCase = true)) {
            tilEmail.error = "El nuevo email no puede ser igual al actual"
            return
        }

        btnEnviarMail.isEnabled = false
        btnEnviarMail.text = "Enviando..."

        firebaseAuth.currentUser?.verifyBeforeUpdateEmail(nuevoEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    tvMensajeExito.text = "Se envió un enlace de verificación a $nuevoEmail. Hacé clic en el enlace para confirmar el cambio."
                    tvMensajeExito.visibility = View.VISIBLE
                    tvInstruccion.visibility = View.GONE
                    tilEmail.visibility = View.GONE
                    btnEnviarMail.visibility = View.GONE
                } else {
                    val errorMessage = task.exception?.message
                    when {
                        errorMessage?.contains("email already in use", ignoreCase = true) == true -> {
                            tilEmail.error = "Este email ya está registrado"
                        }
                        errorMessage?.contains("invalid email", ignoreCase = true) == true -> {
                            tilEmail.error = "Email inválido"
                        }
                        else -> {
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                    btnEnviarMail.isEnabled = true
                    btnEnviarMail.text = "Enviar enlace de verificación"
                }
            }
    }

    private fun validarEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "El email es obligatorio"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Formato de email inválido"
                false
            }
            else -> {
                tilEmail.error = null
                true
            }
        }
    }
}
