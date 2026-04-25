package com.followup.presentation.login

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.followup.fragments.PrincipalActivity
import com.followup.presentation.register.RegistrarCuenta
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class Login : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var tilEmail: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()

        setupBiometric()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // If user has an active Firebase session and biometric enabled,
        // skip the form entirely and prompt biometric directly
        val hasActiveSession = firebaseAuth.currentUser != null
        val biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)

        if (hasActiveSession && biometricEnabled && canAuthenticateWithBiometric()) {
            showBiometricPrompt()
            return  // don't set up the form listeners
        }

        setupForm()
    }

    private fun setupForm() {
        tilEmail = findViewById(R.id.til_Email)
        val tietEmail = findViewById<TextInputEditText>(R.id.tiet_Email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_Password)
        val tietPassword = findViewById<TextInputEditText>(R.id.tiet_Password)
        btnLogin = findViewById(R.id.btn_Login)
        val tvRegister = findViewById<TextView>(R.id.tv_Register)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_ForgotPassword)
        val progressBar = findViewById<View>(R.id.progressBar)

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
            tilEmail.error = null
            progressBar.visibility = View.VISIBLE
            tvForgotPassword.isEnabled = false

            firebaseAuth.sendPasswordResetEmail(email.lowercase())
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    tvForgotPassword.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Enlace enviado. Revisa tu correo.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error al enviar el enlace. Verificá el email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun validarFront(
        email: String, password: String,
        tilEmail: TextInputLayout, tilPassword: TextInputLayout
    ): Boolean {
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

    @SuppressLint("SetTextI18n")
    private fun ejecutarLogin(email: String, password: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "Ingresando..."
        btnLogin.setBackgroundColor(getColor(R.color.primary_blue_disabled))

        val emailLower = email.lowercase()
        firebaseAuth.signInWithEmailAndPassword(emailLower, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.ingresar)
                btnLogin.setBackgroundColor(getColor(R.color.primary_blue))

                if (task.isSuccessful) {
                    val userName = firebaseAuth.currentUser?.displayName ?: "Usuario"

                    sharedPreferences.edit {
                        putString("USER_MAIL", emailLower)
                            .putString("USER_NAME", userName)
                    }

                    navigateToMain()
                } else {
                    val errorMessage = task.exception?.message
                    when {
                        errorMessage?.contains("no user record", ignoreCase = true) == true ->
                            findViewById<TextInputLayout>(R.id.til_Email).error =
                                "Este correo no está registrado"
                        errorMessage?.contains("password", ignoreCase = true) == true ->
                            findViewById<TextInputLayout>(R.id.til_Password).error =
                                "Contraseña incorrecta"
                        else ->
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or hardware error — fall back to the login form
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        setupForm()  // show the password form as fallback
                    } else {
                        Toast.makeText(this@Login, "Error: $errString", Toast.LENGTH_SHORT).show()
                        setupForm()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't navigate — let the prompt handle retries automatically
                    Toast.makeText(this@Login, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun canAuthenticateWithBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Bienvenido de nuevo")
            .setSubtitle("Verificá tu identidad para ingresar")
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, PrincipalActivity::class.java))
        finish()
    }
}