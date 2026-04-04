package com.followup.fragments

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import  androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ReestablecerFragment : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reestablecer_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNueva = findViewById<EditText>(R.id.tiet_NuevaContrasena)
        val etConfirmar = findViewById<EditText>(R.id.tiet_ConfirmPassword)
        val btnConfirmar = findViewById<MaterialButton>(R.id.btn_Confirmar)
        val email = intent.getStringExtra("email")

        btnConfirmar.setOnClickListener {
            val nueva = etNueva.text.toString().trim()
            val confirmar = etConfirmar.text.toString().trim()

            if(nueva.isEmpty() || confirmar.isEmpty()){
                Toast.makeText(this, "Campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(nueva.length < 6){
                Toast.makeText(this, "Minimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(nueva != confirmar) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            actualizarContrasenia(email!!, nueva)
        }
    }
    private fun actualizarContrasenia(email: String, nuevaContrasenia: String) {

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ReestablecerFragment)
                val dao = db.usuarioDao()

                dao.actualizarContrasenia(email, nuevaContrasenia)

                Toast.makeText(this@ReestablecerFragment, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@ReestablecerFragment, "Erro en la base de datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}