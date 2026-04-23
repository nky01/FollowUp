package com.followup.presentation.login

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.content.Context
    // Permite acceder a recursos del sistema (SharedPreferences, Intents, etc.)

import android.content.Intent
    // Sirve para navegar entre Activities

import android.content.SharedPreferences
    // Almacenamiento local clave-valor (guardar sesión, datos simples)

import android.os.Bundle
    // Contenedor de datos que se pasa entre Activities

import android.util.Log
    // Permite imprimir logs en la consola (debug)

import android.util.Patterns
    // Contiene patrones predefinidos (ej: validación de email)

import android.widget.TextView
    // Componente de texto en la UI

import android.widget.Toast
    // Mensajes cortos emergentes en pantalla

import androidx.activity.enableEdgeToEdge
    // Permite usar toda la pantalla (debajo de la barra superior)

import androidx.appcompat.app.AppCompatActivity
    // Clase base para Activities (compatibilidad con versiones antiguas)

import androidx.core.content.edit
    // Extensión para editar SharedPreferences de forma más simple

import androidx.core.view.ViewCompat
    // Utilidades para trabajar con vistas (compatibilidad)

import androidx.core.view.WindowInsetsCompat
    // Manejo de espacios del sistema (status bar, navigation bar)

import androidx.lifecycle.lifecycleScope
    // Scope de corrutinas ligado al ciclo de vida del Activity

import com.followup.R
    // Acceso a recursos del proyecto (layouts, strings, ids, etc.)

import com.followup.data.database.AppDatabase
    // Clase principal de la base de datos ROOM

import com.followup.fragments.PrincipalActivity
    // Activity principal de la app (pantalla luego del login)

import com.followup.fragments.ReestablecerFragment
    // Pantalla para recuperar contraseña

import com.followup.presentation.register.RegistrarCuenta
    // Activity para registrar un nuevo usuario

import com.google.android.material.button.MaterialButton
    // Botón con estilo Material Design

import com.google.android.material.textfield.TextInputEditText
    // Campo de texto editable (Material Design)

import com.google.android.material.textfield.TextInputLayout
    // Contenedor del input (maneja errores, estilos, iconos)

import com.google.firebase.auth.FirebaseAuth
    // Maneja autenticación con Firebase

import kotlinx.coroutines.Dispatchers
    // Define en qué hilo se ejecuta la corrutina (Main, IO, etc.)

import kotlinx.coroutines.launch
    // Permite iniciar una corrutina

import kotlinx.coroutines.withContext
    // Permite cambiar de hilo dentro de una corrutina

/* ----------------------------------------------------------------------------------------
                                        ACTIVITY LOGIN
---------------------------------------------------------------------------------------- */
/*
    [+] - Se ejecuta al finalizar el "Activity" "Bienvenida"
*/

class Login : AppCompatActivity() {

    /* ----------------------------------------------------------------------------------------
                                            ATRIBUTOS
    ---------------------------------------------------------------------------------------- */

    private lateinit var sharedPreferences: SharedPreferences
    // Guarda datos simples en el celular (Sesión de Usuario)

    private lateinit var firebaseAuth: FirebaseAuth
    // Permite autenticar usuarios con Firebase

    private lateinit var database: AppDatabase
    // Base de datos local (ROOM)

    /* ---------- Componentes UI ---------- */

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tietEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tietPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    /* ----------------------------------------------------------------------------------------
                                      MÉTODOS PREDEFINIDOS
    ---------------------------------------------------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setupUI() // Configura UI
        initComponents()
        initListeners()
    }

    /* ----------------------------------------------------------------------------------------
                                          SETUP INICIAL
    ---------------------------------------------------------------------------------------- */

    private fun setupUI() {

        enableEdgeToEdge()
            // Permite usar toda la pantalla (incluye zona del status bar)

        setContentView(R.layout.activity_login)
            // Conecta este Activity con su XML

        applyInsets()
            // Ajusta márgenes para no superponer con barras del sistema
    }

    private fun applyInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Obtiene tamaño de barras del sistema (arriba y abajo)

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                // Aplica padding dinámico

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

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
            // Acceso a almacenamiento local

        firebaseAuth = FirebaseAuth.getInstance()
            // Inicializa Firebase Auth

        database = AppDatabase.getDatabase(this)
            // Inicializa base de datos Room
    }

    private fun initViews() {

        tilEmail = findViewById(R.id.til_Email)
        tietEmail = findViewById(R.id.tiet_Email)

        tilPassword = findViewById(R.id.til_Password)
        tietPassword = findViewById(R.id.tiet_Password)

        btnLogin = findViewById(R.id.btn_Login)

        tvRegister = findViewById(R.id.tv_Register)
        tvForgotPassword = findViewById(R.id.tv_ForgotPassword)
    }

    /* ----------------------------------------------------------------------------------------
                                      LISTENERS (EVENTOS)
    ---------------------------------------------------------------------------------------- */

    private fun initListeners() {

        setupRegisterListener()
        setupLoginListener()
        setupForgotPasswordListener()
    }

    private fun setupRegisterListener() {

        tvRegister.setOnClickListener {

            startActivity(Intent(this, RegistrarCuenta::class.java))
                // Navega a pantalla de registro
        }
    }

    private fun setupLoginListener() {

        btnLogin.setOnClickListener {

            val email = tietEmail.text.toString().trim()
            val password = tietPassword.text.toString().trim()

            if (validarFront(email, password)) {
                ejecutarLogin(email, password)
            }
        }
    }

    private fun setupForgotPasswordListener() {

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

    /* ----------------------------------------------------------------------------------------
                                      VALIDACIONES
    ---------------------------------------------------------------------------------------- */

    private fun validarFront(email: String, password: String): Boolean {

        limpiarErrores()

        var esValido = true

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
        }

        return esValido
    }

    private fun limpiarErrores() {

        tilEmail.error = null
        tilPassword.error = null
            // Limpia errores previos antes de validar
    }

    /* ----------------------------------------------------------------------------------------
                                      LOGIN (FIREBASE)
    ---------------------------------------------------------------------------------------- */

    private fun ejecutarLogin(email: String, password: String) {

        val emailLower = email.lowercase()
            // Normaliza email para evitar problemas de comparación

        firebaseAuth.signInWithEmailAndPassword(emailLower, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    onLoginSuccess(emailLower)

                } else {

                    manejarErrorLogin(task.exception?.message)
                }
            }
    }

    private fun onLoginSuccess(email: String) {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val usuario = database.usuarioDao().obtenerPorMail(email)

                guardarSesionYEntrar(usuario?.nombre ?: "Usuario", email)

            } catch (e: Exception) {

                Log.e("LOGIN_ERROR", "DB error: ${e.message}")

                mostrarError("Error al cargar datos")
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      POST LOGIN
    ---------------------------------------------------------------------------------------- */

    private suspend fun guardarSesionYEntrar(nombre: String, email: String) {

        withContext(Dispatchers.Main) {

            sharedPreferences.edit {
                putString("USER_MAIL", email)
                putString("USER_NAME", nombre)
            }

            startActivity(Intent(this@Login, PrincipalActivity::class.java))
                // Navega a pantalla principal

            finish()
                // Cierra login
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      MANEJO DE ERRORES
    ---------------------------------------------------------------------------------------- */

    private fun manejarErrorLogin(errorMessage: String?) {

        when {
            errorMessage?.contains("no user record", true) == true -> {
                tilEmail.error = "Este correo no está registrado"
            }

            errorMessage?.contains("wrong password", true) == true ||
                    errorMessage?.contains("password", true) == true -> {
                tilPassword.error = "Contraseña incorrecta"
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
