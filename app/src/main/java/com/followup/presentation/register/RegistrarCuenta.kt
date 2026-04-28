package com.followup.presentation.register

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.os.Bundle
// Contenedor de datos del Activity

import android.util.Log
// Logs para debug

import android.util.Patterns
// Validaciones predefinidas (email)

import android.widget.ImageButton
// Botón de imagen (volver atrás)

import android.widget.Toast
// Mensajes cortos en pantalla

import androidx.activity.enableEdgeToEdge
// Permite usar toda la pantalla

import androidx.appcompat.app.AppCompatActivity
// Activity base

import androidx.core.view.ViewCompat
// Compatibilidad de vistas

import androidx.core.view.WindowInsetsCompat
// Manejo de barras del sistema

import androidx.lifecycle.lifecycleScope
// Corrutinas ligadas al ciclo de vida

import com.followup.R
// Recursos del proyecto

import com.followup.data.database.AppDatabase
// Base de datos ROOM

import com.followup.data.entity.Usuario
// Entidad Usuario

import com.google.android.material.button.MaterialButton
// Botón Material

import com.google.android.material.textfield.TextInputEditText
// Input editable

import com.google.android.material.textfield.TextInputLayout
// Contenedor del input (manejo de errores)

import com.google.firebase.auth.FirebaseAuth
// Autenticación Firebase

import kotlinx.coroutines.Dispatchers
// Manejo de hilos

import kotlinx.coroutines.launch
// Iniciar corrutinas

import kotlinx.coroutines.withContext
import androidx.core.content.edit

// Cambiar de hilo

/* ----------------------------------------------------------------------------------------
                                  ACTIVITY REGISTRAR CUENTA
---------------------------------------------------------------------------------------- */
/*
    [+] Permite crear un nuevo usuario:
        - Registra en Firebase (autenticación)
        - Guarda usuario en Room (base local)
        - Guarda nombre en SharedPreferences
*/

class RegistrarCuenta : AppCompatActivity() {

    /* ----------------------------------------------------------------------------------------
                                            ATRIBUTOS
    ---------------------------------------------------------------------------------------- */

    private lateinit var firebaseAuth: FirebaseAuth
    // Maneja autenticación Firebase

    private lateinit var database: AppDatabase
    // Base de datos local (ROOM)

    /* ---------- Componentes UI ---------- */

    private lateinit var tilName: TextInputLayout
    private lateinit var tietName: TextInputEditText

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tietEmail: TextInputEditText

    private lateinit var tilPassword: TextInputLayout
    private lateinit var tietPassword: TextInputEditText

    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tietConfirmPassword: TextInputEditText

    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBack: ImageButton

    /* ----------------------------------------------------------------------------------------
                                      MÉTODOS PREDEFINIDOS
    ---------------------------------------------------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
        initComponents()
        initListeners()
    }

    /* ----------------------------------------------------------------------------------------
                                          SETUP INICIAL
    ---------------------------------------------------------------------------------------- */

    private fun setupUI() {

        enableEdgeToEdge()
        // Usa toda la pantalla

        setContentView(R.layout.activity_registrar_cuenta)
        // Conecta XML

        applyInsets()
        // Ajusta márgenes del sistema
    }

    private fun applyInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            insets
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      INICIALIZACIÓN
    ---------------------------------------------------------------------------------------- */

    private fun initComponents() {
        initServices()
        initViews()
    }

    private fun initServices() {

        firebaseAuth = FirebaseAuth.getInstance()
        // Inicializa Firebase Auth

        database = AppDatabase.getDatabase(this)
        // Inicializa ROOM
    }

    private fun initViews() {

        tilName = findViewById(R.id.til_Name)
        tietName = findViewById(R.id.tiet_Name)

        tilEmail = findViewById(R.id.til_Email)
        tietEmail = findViewById(R.id.tiet_Email)

        tilPassword = findViewById(R.id.til_Password)
        tietPassword = findViewById(R.id.tiet_Password)

        tilConfirmPassword = findViewById(R.id.til_ConfirmPassword)
        tietConfirmPassword = findViewById(R.id.tiet_ConfirmPassword)

        btnRegister = findViewById(R.id.btn_Register)
        btnBack = findViewById(R.id.btn_Back)
    }

    /* ----------------------------------------------------------------------------------------
                                      LISTENERS (EVENTOS)
    ---------------------------------------------------------------------------------------- */

    private fun initListeners() {

        btnBack.setOnClickListener { finish() }
        // Vuelve a la pantalla anterior

        btnRegister.setOnClickListener {
            manejarRegistro()
        }
    }

    private fun manejarRegistro() {

        val nombre = tietName.text.toString().trim()
        val email = tietEmail.text.toString().trim()
        val password = tietPassword.text.toString().trim()
        val confirmPassword = tietConfirmPassword.text.toString().trim()

        if (validarFrontend(nombre, email, password, confirmPassword)) {
            registrarUsuario(nombre, email, password)
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      VALIDACIONES
    ---------------------------------------------------------------------------------------- */

    private fun validarFrontend(
        nombre: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        limpiarErrores()

        var esValido = true

        if (nombre.isEmpty()) {
            tilName.error = "El nombre es obligatorio"
            esValido = false
        }

        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email inválido"
            esValido = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            esValido = false
        } else {
            val errorContrasenia = validarContrasenia(password)
            if (errorContrasenia != null) {
                tilPassword.error = errorContrasenia
                esValido = false
            }
        }

        if (confirmPassword != password) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            esValido = false
        }

        return esValido
    }

    private fun validarContrasenia(password: String): String? {
        val errores = mutableListOf<String>()

        if (password.length < 8) {
            errores.add("mínimo 8 caracteres")
        }
        if (!password.any { it.isLetter() }) {
            errores.add("al menos una letra")
        }
        if (!password.any { it.isDigit() }) {
            errores.add("al menos un número")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            errores.add("al menos un carácter especial (!@#$...)")
        }

        return if (errores.isEmpty()) null
        else "La contraseña debe tener: ${errores.joinToString(", ")}"
    }

    private fun limpiarErrores() {
        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }

    /* ----------------------------------------------------------------------------------------
                                      REGISTRO (FIREBASE + ROOM)
    ---------------------------------------------------------------------------------------- */

    private fun registrarUsuario(nombre: String, email: String, password: String) {

        val emailLower = email.lowercase()
        // Normaliza email

        firebaseAuth.createUserWithEmailAndPassword(emailLower, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    onRegisterSuccess(nombre, emailLower, password)
                } else {
                    manejarErrorRegistro(task.exception?.message)
                }
            }
    }

    private fun onRegisterSuccess(nombre: String, email: String, password: String) {

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
                    // Guarda en ROOM

                    guardarNombreEnPrefs(nombre)
                }

                mostrarExito()

            } catch (e: Exception) {

                Log.e("REGISTRO_ERROR", "DB Error: ${e.message}")
                mostrarError("Error al guardar en base local")
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      POST REGISTRO
    ---------------------------------------------------------------------------------------- */

    private suspend fun mostrarExito() {

        withContext(Dispatchers.Main) {

            Toast.makeText(this@RegistrarCuenta, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()

            finish()
            // Vuelve al login
        }
    }

    private fun guardarNombreEnPrefs(nombre: String) {

        val prefs = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)

        prefs.edit {
            putString("USER_NAME", nombre)
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      MANEJO DE ERRORES
    ---------------------------------------------------------------------------------------- */

    private fun manejarErrorRegistro(errorMessage: String?) {

        when {
            errorMessage?.contains("email already in use", true) == true -> {
                tilEmail.error = "Este correo ya está en uso"
            }

            errorMessage?.contains("weak password", true) == true -> {
                tilPassword.error = "Contraseña muy débil"
            }

            else -> {
                mostrarError("Error: $errorMessage")
            }
        }
    }

    private fun mostrarError(mensaje: String) {

        runOnUiThread {
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }
}