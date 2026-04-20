package com.followup.fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PrincipalActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseEmail = firebaseAuth.currentUser?.email?.lowercase()
        val savedEmail = sharedPreferences.getString("USER_MAIL", "")?.lowercase()

        if (firebaseEmail != null && firebaseEmail != savedEmail) {
            lifecycleScope.launch {
                val database = AppDatabase.getDatabase(applicationContext)
                database.usuarioDao().actualizarMail(savedEmail ?: "", firebaseEmail)
                sharedPreferences.edit().putString("USER_MAIL", firebaseEmail).apply()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.addAuthStateListener(authStateListener)

        initComponent()

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

        replaceFragment(InicioFragment())

    }

    private fun initComponent(){
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

}