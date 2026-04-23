package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.content.SharedPreferences
// Almacenamiento local clave-valor (guardar sesión)

import android.os.Bundle
// Contenedor de datos del Activity

import androidx.appcompat.app.AppCompatActivity
// Clase base para Activities

import androidx.fragment.app.Fragment
// Base para manejar fragments

import androidx.lifecycle.lifecycleScope
// Scope de corrutinas ligado al ciclo de vida

import com.followup.R
// Recursos del proyecto (layouts, ids, etc.)

import com.followup.data.database.AppDatabase
// Base de datos ROOM

import com.followup.fragments.*
// Fragments de navegación (Inicio, Clientes, etc.)

import com.google.android.material.bottomnavigation.BottomNavigationView
// Barra de navegación inferior

import com.google.firebase.auth.FirebaseAuth
// Manejo de autenticación con Firebase

import kotlinx.coroutines.launch
// Corrutinas

/* ----------------------------------------------------------------------------------------
                                   ACTIVITY PRINCIPAL
---------------------------------------------------------------------------------------- */
/*
    [+] - Activity principal de la app luego del login
    [+] - Maneja navegación entre fragments
    [+] - Sincroniza estado de usuario con Firebase
*/

class PrincipalActivity : AppCompatActivity() {

    /* ----------------------------------------------------------------------------------------
                                            ATRIBUTOS
    ---------------------------------------------------------------------------------------- */

    private lateinit var bottomNavigationView: BottomNavigationView
    // Barra de navegación inferior

    private lateinit var firebaseAuth: FirebaseAuth
    // Manejo de autenticación Firebase

    private lateinit var sharedPreferences: SharedPreferences
    // Almacenamiento local (sesión usuario)

    /* ---------- Listener de autenticación ---------- */

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->

        val firebaseEmail = firebaseAuth.currentUser?.email?.lowercase()
        // Email actual de Firebase

        val savedEmail = sharedPreferences.getString("USER_MAIL", "")?.lowercase()
        // Email guardado localmente

        // Si cambió el email en Firebase → sincronizar con Room y SharedPreferences
        if (firebaseEmail != null && firebaseEmail != savedEmail) {

            lifecycleScope.launch {

                val database = AppDatabase.getDatabase(applicationContext)

                database.usuarioDao().actualizarMail(
                    savedEmail ?: "",
                    firebaseEmail
                )

                sharedPreferences.edit()
                    .putString("USER_MAIL", firebaseEmail)
                    .apply()
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      MÉTODOS PREDEFINIDOS
    ---------------------------------------------------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setupUI()
        initComponents()
        initListeners()
        initDefaultFragment()
    }

    override fun onDestroy() {
        super.onDestroy()

        firebaseAuth.removeAuthStateListener(authStateListener)
        // Evita fugas de memoria (memory leaks)
    }

    /* ----------------------------------------------------------------------------------------
                                          SETUP INICIAL
    ---------------------------------------------------------------------------------------- */

    private fun setupUI() {

        setContentView(R.layout.activity_principal)
        // Conecta con XML principal
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
        // Inicializa almacenamiento local

        firebaseAuth = FirebaseAuth.getInstance()
        // Inicializa Firebase Auth

        firebaseAuth.addAuthStateListener(authStateListener)
        // Escucha cambios en autenticación
    }

    private fun initViews() {

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        // Inicializa Bottom Navigation
    }

    /* ----------------------------------------------------------------------------------------
                                      LISTENERS (EVENTOS)
    ---------------------------------------------------------------------------------------- */

    private fun initListeners() {

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {

        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {

                R.id.bottom_Inicio -> {
                    replaceFragment(InicioFragment())
                    true
                }

                R.id.bottom_Clientes -> {
                    replaceFragment(ClientesFragment())
                    true
                }

                R.id.bottom_Ventas -> {
                    replaceFragment(VentasFragment())
                    true
                }

                R.id.bottom_Historial -> {
                    replaceFragment(HistorialFragment())
                    true
                }

                else -> false
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      NAVEGACIÓN
    ---------------------------------------------------------------------------------------- */

    private fun initDefaultFragment() {

        replaceFragment(InicioFragment())
        // Fragment inicial al abrir la app
    }

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager = supportFragmentManager

        val currentFragment = fragmentManager.findFragmentById(R.id.frame_container)

        // Evita recargar el mismo fragment innecesariamente
        if (currentFragment?.javaClass == fragment.javaClass) return

        fragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .setReorderingAllowed(true) // Optimiza transiciones
            .commit()
    }
}
