package com.followup.fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.followup.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class PrincipalActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

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

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager

        val currentFragment = fragmentManager.findFragmentById(R.id.frame_container)
        if (currentFragment?.javaClass == fragment.javaClass) return

        fragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .setReorderingAllowed(true) // Optimiza las transiciones
            .commit()
    }

}