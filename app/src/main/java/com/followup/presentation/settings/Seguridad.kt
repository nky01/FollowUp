package com.followup.presentation.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.Executor
import androidx.core.content.edit

class Seguridad : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var switchBiometrico: SwitchMaterial

    companion object {
        private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
        private fun showBiometricPrompt(seguridad: Seguridad) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verificación biométrica")
                .setSubtitle("Verifica tu identidad para habilitar el desbloqueo biométrico")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            seguridad.biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seguridad)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBiometric()
        setupUI()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@Seguridad, "Error: $errString", Toast.LENGTH_SHORT).show()
                    switchBiometrico.isChecked = false
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val isEnabled = sharedPreferences.getBoolean(BIOMETRIC_ENABLED_KEY, false)
                    if (!isEnabled) {
                        sharedPreferences.edit {
                            putBoolean(BIOMETRIC_ENABLED_KEY, true)
                        }
                        Toast.makeText(this@Seguridad, "Huella habilitada", Toast.LENGTH_SHORT).show()
                    }
                    switchBiometrico.isChecked = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@Seguridad, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.ll_CambiarContrasenia).setOnClickListener {
            startActivity(Intent(this, CambiarContrasenia::class.java))
        }

        findViewById<LinearLayout>(R.id.ll_SegundoFactor).setOnClickListener {
            // TODO: Navegar a pantalla de segundo factor
        }

        switchBiometrico = findViewById(R.id.switchBiometrico)
        switchBiometrico.isChecked = sharedPreferences.getBoolean(BIOMETRIC_ENABLED_KEY, false)

        switchBiometrico.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (canAuthenticateWithBiometric()) {
                    showBiometricPrompt(this)
                } else {
                    switchBiometrico.isChecked = false
                    Toast.makeText(this, "Huella no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                }
            } else {
                sharedPreferences.edit {
                    putBoolean(BIOMETRIC_ENABLED_KEY, false)
                }
                Toast.makeText(this, "Huella deshabilitada", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun canAuthenticateWithBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

}