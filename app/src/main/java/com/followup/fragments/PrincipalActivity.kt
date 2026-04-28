package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.content.Intent
import android.content.SharedPreferences
// Almacenamiento local clave-valor (guardar sesión)

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
// Contenedor de datos del Activity

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
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
import com.followup.fragments.menuLateral.AgendaFragment
import com.followup.fragments.menuLateral.ConfiguracionFragment
import com.followup.fragments.menuLateral.EstadisticasFragment
import com.followup.presentation.login.Login
import com.followup.presentation.settings.Configuracion
import com.followup.presentation.settings.SessionManager
// Fragments de navegación (Inicio, Clientes, etc.)

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
// Barra de navegación inferior

import com.google.firebase.auth.FirebaseAuth
// Manejo de autenticación con Firebase

import kotlinx.coroutines.launch
// Corrutinas

import androidx.appcompat.app.AppCompatDelegate

/* ----------------------------------------------------------------------------------------
                                   ACTIVITY PRINCIPAL
---------------------------------------------------------------------------------------- */
/*
    [+] Activity principal luego del login
    [+] Maneja navegación entre fragments (Bottom + Drawer)
    [+] Oculta o muestra el BottomNavigation según la sección
    [+] Sincroniza estado de usuario con Firebase
*/

class PrincipalActivity : AppCompatActivity() {

    /* ----------------------------------------------------------------------------------------
                                            UI
    ---------------------------------------------------------------------------------------- */

    private lateinit var bottomNavigationView: BottomNavigationView
    // Navegación inferior principal

    private lateinit var drawerLayout: DrawerLayout
    // Contenedor del menú lateral

    private lateinit var navigationView: NavigationView
    // Menú lateral (drawer)

    private lateinit var btnCerrarSesion: TextView
    // Botón footer del drawer (logout)

    private lateinit var txtName: TextView
    // Nombre del usuario en el header del menú lateral

    private lateinit var profileImageDrawer: de.hdodenhof.circleimageview.CircleImageView
    // Contenedor de imagen del menú lateral

    /* ----------------------------------------------------------------------------------------
                                            SERVICIOS
    ---------------------------------------------------------------------------------------- */

    private lateinit var firebaseAuth: FirebaseAuth
    // Autenticación Firebase

    private lateinit var sharedPreferences: SharedPreferences
    // Persistencia local de usuario

    /* ----------------------------------------------------------------------------------------
                              AUTH LISTENER (SYNC USUARIO)
    ---------------------------------------------------------------------------------------- */

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->

        val firebaseEmail = firebaseAuth.currentUser?.email?.lowercase()
        val savedEmail = sharedPreferences.getString("USER_MAIL", "")?.lowercase()

        // Si el usuario cambió en Firebase, sincroniza DB local
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
                                      CICLO DE VIDA
    ---------------------------------------------------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_principal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.content_container)
                .setPadding(0, systemBars.top, 0, 0)

            insets
        }


        val prefs = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        setupUI()
        initComponents()
        syncUserNameIfNeeded()
        initListeners()
        initDefaultFragment()

        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.frame_container)

            when (fragment) {
                is InicioFragment,
                is ClientesFragment,
                is VentasFragment,
                is HistorialFragment -> showBottomNavigation(true)

                else -> showBottomNavigation(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarFotoPerfilDrawer()
    }

    private fun syncUserNameIfNeeded() {
        val userMail = sharedPreferences.getString("USER_MAIL", null) ?: return

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val usuario = db.usuarioDao().obtenerPorMail(userMail)
                val nombre = usuario?.nombre

                if (nombre.isNullOrBlank()) return@launch  // don't overwrite with empty

                sharedPreferences.edit {
                    putString("USER_NAME", nombre)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    /* ----------------------------------------------------------------------------------------
                                          SETUP UI
    ---------------------------------------------------------------------------------------- */

    private fun setupUI() {
        setContentView(R.layout.activity_principal)
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

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    private fun initViews() {

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        drawerLayout = findViewById(R.id.main)
        navigationView = findViewById(R.id.navigationView)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        // ---------- Header del Drawer ----- */
        val headerView = navigationView.getHeaderView(0)
        txtName = headerView.findViewById(R.id.txtName)

        // ---------- Imagen del Drawer ----- */
        profileImageDrawer = headerView.findViewById(R.id.profilePicture)

        // ---------- Cargar Nombre de Usuario ----- */
        cargarNombreUsuario()

        // ---------- Cargar Imagen del Drawer ----- */
        cargarFotoPerfilDrawer()

    }

    /* ----------------------------------------------------------------------------------------
                                      LISTENERS
    ---------------------------------------------------------------------------------------- */

    private fun initListeners() {
        setupBottomNavigation()
        setupDrawer()
        setupCerrarSesion()
    }

    /* ----------------------------------------------------------------------------------------
                              BOTTOM NAVIGATION
    ---------------------------------------------------------------------------------------- */

    private fun setupBottomNavigation() {

        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {

                R.id.bottom_Inicio -> {
                    // Este es el fragment principal que contiene tu lógica general
                    showBottomNavigation(true)
                    replaceFragment(InicioFragment())
                    true
                }

                R.id.bottom_Clientes -> {
                    showBottomNavigation(true)
                    replaceFragment(ClientesFragment())
                    true
                }

                R.id.bottom_Ventas -> {
                    showBottomNavigation(true)
                    replaceFragment(VentasFragment())
                    true
                }

                R.id.bottom_Historial -> {
                    showBottomNavigation(true)
                    replaceFragment(HistorialFragment())
                    true
                }

                else -> false
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                              DRAWER NAVIGATION
    ---------------------------------------------------------------------------------------- */

    private fun setupDrawer() {

        navigationView.setNavigationItemSelectedListener {

            when (it.itemId) {

                R.id.nav_inicio -> {
                    // Vuelve a la pantalla principal y muestra el bottom nav
                    showBottomNavigation(true)
                    replaceFragment(InicioFragment())
                }

                R.id.nav_agenda -> {
                    // Sección del drawer: oculta el bottom nav
                    showBottomNavigation(false)
                    replaceFragment(AgendaFragment())
                }

                R.id.nav_estadisticas -> {
                    showBottomNavigation(false)
                    replaceFragment(EstadisticasFragment())
                }

                R.id.nav_configuracion -> {
                    startActivity(Intent(this, Configuracion::class.java))
                }
            }

            drawerLayout.closeDrawers()
            true
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      CARGAR NOMBRE DE USUARIO
    ---------------------------------------------------------------------------------------- */

    private fun cargarNombreUsuario() {

        // Tomo el mail que guardaste al loguearte
        val mail = sharedPreferences.getString("USER_MAIL", "") ?: ""

        if (mail.isBlank()) {
            txtName.text = "Usuario"
            return
        }

        lifecycleScope.launch {

            val database = AppDatabase.getDatabase(applicationContext)
            val usuario = database.usuarioDao().obtenerPorMail(mail)

            // Si encuentra el usuario, muestra su nombre
            txtName.text = usuario?.nombre ?: "Usuario"
        }
    }

    /* ----------------------------------------------------------------------------------------
                              CERRAR SESIÓN
    ---------------------------------------------------------------------------------------- */

    private fun setupCerrarSesion() {
        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {

        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnAccept = dialogView.findViewById<Button>(R.id.btnAccept)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAccept.setOnClickListener {

            firebaseAuth.signOut()

            val intent = Intent(this, Login::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }

        dialog.show()

        // 🔥 quitar fondo por defecto del dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    /* ----------------------------------------------------------------------------------------
                              NAVEGACIÓN PRINCIPAL
    ---------------------------------------------------------------------------------------- */

    private fun initDefaultFragment() {
        // Pantalla inicial: PrincipalFragment con bottom visible
        showBottomNavigation(true)
        replaceFragment(InicioFragment())
    }

    private fun replaceFragment(fragment: Fragment) {

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.frame_container)

        // Evita recargar el mismo fragment
        if (currentFragment?.javaClass == fragment.javaClass) return

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
    }

    /* ----------------------------------------------------------------------------------------
                                 VISIBILIDAD DEL BOTTOM NAV
    ---------------------------------------------------------------------------------------- */

    private fun showBottomNavigation(visible: Boolean) {
        bottomNavigationView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /* ----------------------------------------------------------------------------------------
                                    CARGAR FOTO DE PERFIL
    ---------------------------------------------------------------------------------------- */

    private fun cargarFotoPerfilDrawer() {

        val uriString = sharedPreferences.getString("PROFILE_IMAGE_URI", null)

        if (uriString != null) {
            val uri = android.net.Uri.parse(uriString)

            com.bumptech.glide.Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profileImageDrawer)
        } else {
            profileImageDrawer.setImageResource(R.drawable.ic_person)
        }
    }
}
