package com.followup.presentation.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.followup.R
import com.followup.presentation.login.Bienvenida
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit
import com.bumptech.glide.Glide
import androidx.core.net.toUri

class Configuracion : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var imagePicker: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var profileImage: de.hdodenhof.circleimageview.CircleImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)

        sharedPreferences = getSharedPreferences("FollowUp_prefs", MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()

        profileImage = findViewById(R.id.profilePicture)
        val container = findViewById<com.google.android.material.card.MaterialCardView>(R.id.profileContainer)

        // Registrar selector de imagen
        imagePicker = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // algunos dispositivos no lo permiten
                }

                // Mostrar imagen
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(profileImage)

                // Guardar URI
                sharedPreferences.edit {
                    putString("PROFILE_IMAGE_URI", uri.toString())
                }
            }
        }

        container.setOnClickListener {
            imagePicker.launch("image/*")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        findViewById<TextView>(R.id.tvUserName).text = userName

        val savedUri = sharedPreferences.getString("PROFILE_IMAGE_URI", null)

        if (savedUri != null) {
            Glide.with(this)
                .load(savedUri.toUri())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profileImage)
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<LinearLayout>(R.id.ll_CambiarMail).setOnClickListener {
            startActivity(Intent(this, CambiarMail::class.java))
        }

        findViewById<LinearLayout>(R.id.llSeguridad).setOnClickListener {
            startActivity(Intent(this, Seguridad::class.java))
        }

        findViewById<LinearLayout>(R.id.ll_CerrarSesion).setOnClickListener {
            firebaseAuth.signOut()
            sharedPreferences.edit { clear() }
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Bienvenida::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.ll_Apariencia).setOnClickListener {
            mostrarDialogApariencia()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        /* Localización */
        val tvUbicacion = findViewById<TextView>(R.id.tvUbicacion)

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionYMostrar(tvUbicacion)
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty()) {
            val granted = grantResults.any { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (granted) {
                val tvUbicacion = findViewById<TextView>(R.id.tvUbicacion)
                obtenerUbicacionYMostrar(tvUbicacion)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionYMostrar(tv: TextView) {

        val fusedLocationClient = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->

            if (location != null) {

                try {
                    val geocoder = android.location.Geocoder(this, java.util.Locale("es", "AR"))

                    val direcciones = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )

                    if (!direcciones.isNullOrEmpty()) {
                        val dir = direcciones[0]

                        val ciudad = dir.locality ?: dir.subAdminArea ?: dir.adminArea ?: "Argentina"
                        val pais = dir.countryName ?: ""

                        tv.text = "$ciudad, $pais"
                    } else {
                        tv.text = "Argentina"
                    }

                } catch (_: Exception) {
                    tv.text = "Argentina"
                }

            } else {
                tv.text = "Argentina"
            }
        }.addOnFailureListener {
            tv.text = "Argentina"
        }
    }

    private fun mostrarDialogApariencia() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_apariencia, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val switchDark = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_dark_mode)

        // Cargar preferencia guardada
        val isDark = sharedPreferences.getBoolean("dark_mode", false)
        switchDark.isChecked = isDark

        switchDark.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("dark_mode", isChecked) }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cerrar_apariencia)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}