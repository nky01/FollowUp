package com.followup.presentation.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
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
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class Login : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var btnLogin: MaterialButton
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    
    private var pendingEmail: String = ""
    private var pendingUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)
        
        setupBiometric()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tilEmail = findViewById<TextInputLayout>(R.id.til_Email)
        val tietEmail = findViewById<TextInputEditText>(R.id.tiet_Email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_Password)
        val tietPassword = findViewById<TextInputEditText>(R.id.tiet_Password)
        btnLogin = findViewById<MaterialButton>(R.id.btn_Login)
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
                    lifecycleScope.launch {
                        try {
                            val dao = database.usuarioDao()
                            val usuario = dao.obtenerPorMail(emailLower)
                            
                            val userName = usuario?.nombre ?: "Usuario"
                            
                            sharedPreferences.edit {
                                putString("USER_MAIL", emailLower)
                                putString("USER_NAME", userName)
                            }
                            
                            pendingEmail = emailLower
                            pendingUserName = userName
                            
                            val biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)
                            if (biometricEnabled && canAuthenticateWithBiometric()) {
                                showBiometricPrompt()
                            } else {
                                navigateToMain()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(this@Login, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message
                    when {
                        errorMessage?.contains("no user record", ignoreCase = true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Email).error = "Este correo no está registrado"
                        }
                        errorMessage?.contains("wrong password", ignoreCase = true) == true ||
                        errorMessage?.contains("password", ignoreCase = true) == true -> {
                            findViewById<TextInputLayout>(R.id.til_Password).error = "Contraseña incorrecta"
                        }
                        else -> {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
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
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@Login, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@Login, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun canAuthenticateWithBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificación biométrica")
            .setSubtitle("Verifica tu identidad para acceder a la app")
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToMain() {
        val intent = Intent(this@Login, PrincipalActivity::class.java)
        startActivity(intent)
        finish()
    }
}
