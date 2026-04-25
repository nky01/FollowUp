package com.followup.presentation.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.followup.fragments.PrincipalActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class Bienvenida : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida)
        
        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBiometric()
        checkUserSession()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@Bienvenida, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                    navigateToLogin()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@Bienvenida, "Huella no reconocida", Toast.LENGTH_SHORT).show()
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

    private fun checkUserSession() {
        val currentUser = firebaseAuth.currentUser
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                val biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)
                if (biometricEnabled && canAuthenticateWithBiometric()) {
                    showBiometricPrompt()
                } else {
                    navigateToMain()
                }
            } else {
                navigateToLogin()
            }
        }, 1500)
    }

    private fun navigateToMain() {
        val intent = Intent(this, PrincipalActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}
