package com.followup.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.io.File

class PerfilFragment : Fragment() {

    private var usuarioId: Int = -1
    private var profileImageUri: Uri? = null

    // ACA estaba el error, el permiso de leer el almacenamiento externo no se estaba solicitando, por eso no se podia acceder a la imagen seleccionada.
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Solicitar persistencia de permisos para la URI si es necesario
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Algunos proveedores no soportan permisos persistentes
            }
            profileImageUri = uri
            updateProfileImage(uri)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && profileImageUri != null) {
            updateProfileImage(profileImageUri!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
        usuarioId = prefs.getInt("USER_ID", -1)

        setupViews(view)
        loadUserData(view)
    }

    private fun setupViews(view: View) {
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<ImageView>(R.id.btn_settings)?.setOnClickListener {
            Toast.makeText(requireContext(), "Configuración", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<CircleImageView>(R.id.profile_image)?.setOnClickListener {
            showEditProfilePhotoDialog()
        }

        view.findViewById<TextView>(R.id.tv_informacion_personal)?.setOnClickListener {
            showEditPersonalInfoDialog()
        }

        view.findViewById<TextView>(R.id.tv_cambiar_mail)?.setOnClickListener {
            showEditEmailDialog()
        }
    }

    private fun loadUserData(view: View) {
        if (usuarioId == -1) return

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val usuario = db.usuarioDao().obtenerUsuarioById(usuarioId)

                if (usuario != null) {
                    view.findViewById<TextView>(R.id.tv_nombre_usuario)?.text = usuario.nombre
                    
                    if (!usuario.imagenPerfil.isNullOrEmpty()) {
                        try {
                            val uri = Uri.parse(usuario.imagenPerfil)
                            view.findViewById<CircleImageView>(R.id.profile_image)?.setImageURI(uri)
                            
                            val userDataPrefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
                            userDataPrefs.edit().putString("profile_image_uri", usuario.imagenPerfil).apply()
                        } catch (e: Exception) {
                            view.findViewById<CircleImageView>(R.id.profile_image)?.setImageResource(android.R.color.darker_gray)
                        }
                    } else {
                        view.findViewById<CircleImageView>(R.id.profile_image)?.setImageResource(android.R.color.darker_gray)
                    }
                }
            } catch (e: Exception) {
                view.findViewById<TextView>(R.id.tv_nombre_usuario)?.text = "Usuario"
            }
        }
    }

    private fun showEditProfilePhotoDialog() {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_cambiar_foto_perfil)
            dialog.window?.setBackgroundDrawable(requireContext().getDrawable(android.R.drawable.dialog_frame))
            dialog.window?.setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            dialog.findViewById<MaterialButton>(R.id.btn_desde_galeria)?.setOnClickListener {
                pickImageLauncher.launch("image/*")
                dialog.dismiss()
            }

            dialog.findViewById<MaterialButton>(R.id.btn_desde_camera)?.setOnClickListener {
                createImageFile()?.let { file ->
                    profileImageUri = Uri.fromFile(file)
                    cameraLauncher.launch(profileImageUri!!)
                }
                dialog.dismiss()
            }

            dialog.findViewById<MaterialButton>(R.id.btn_cancelar)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = requireContext().cacheDir
            File.createTempFile("profile_pic_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateProfileImage(uri: Uri) {
        val uriString = uri.toString()
        if (usuarioId == -1) return
        
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val usuario = db.usuarioDao().obtenerUsuarioById(usuarioId)
                if (usuario != null) {
                    val usuarioActualizado = usuario.copy(imagenPerfil = uriString)
                    db.usuarioDao().actualizarUsuario(usuarioActualizado)
                    
                    view?.findViewById<CircleImageView>(R.id.profile_image)?.setImageURI(uri)
                    
                    val sharedPref = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("profile_image_uri", uriString).apply()

                    Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditPersonalInfoDialog() {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_editar_informacion_personal)
            dialog.window?.setBackgroundDrawable(requireContext().getDrawable(android.R.drawable.dialog_frame))
            dialog.window?.setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val etNombre = dialog.findViewById<EditText>(R.id.et_nombre)
            val etApellido = dialog.findViewById<EditText>(R.id.et_apellido)
            val btnGuardar = dialog.findViewById<MaterialButton>(R.id.btn_guardar)
            val btnCancelar = dialog.findViewById<MaterialButton>(R.id.btn_cancelar)

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(requireContext())
                    val usuario = db.usuarioDao().obtenerUsuarioById(usuarioId)

                    if (usuario != null) {
                        val nombreParts = usuario.nombre.split(" ")
                        etNombre?.setText(nombreParts.getOrNull(0) ?: "")
                        etApellido?.setText(nombreParts.drop(1).joinToString(" "))
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }

            btnGuardar?.setOnClickListener {
                val nombre = etNombre?.text.toString().trim()
                val apellido = etApellido?.text.toString().trim()

                if (nombre.isEmpty() || apellido.isEmpty()) {
                    Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                } else {
                    val nombreCompleto = "$nombre $apellido"
                    lifecycleScope.launch {
                        try {
                            val db = AppDatabase.getDatabase(requireContext())
                            val usuario = db.usuarioDao().obtenerUsuarioById(usuarioId)
                            if (usuario != null) {
                                val usuarioActualizado = usuario.copy(nombre = nombreCompleto)
                                db.usuarioDao().actualizarUsuario(usuarioActualizado)
                                view?.findViewById<TextView>(R.id.tv_nombre_usuario)?.text = nombreCompleto
                                Toast.makeText(requireContext(), "Información actualizada", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            btnCancelar?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditEmailDialog() {
        try {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_cambiar_email)
            dialog.window?.setBackgroundDrawable(requireContext().getDrawable(android.R.color.transparent))

            val etNuevoEmail = dialog.findViewById<EditText>(R.id.et_nuevo_email)
            val btnGuardar = dialog.findViewById<MaterialButton>(R.id.btn_guardar)
            val btnCancelar = dialog.findViewById<MaterialButton>(R.id.btn_cancelar)

            btnGuardar?.setOnClickListener {
                val nuevoEmail = etNuevoEmail?.text.toString()

                if (nuevoEmail.isEmpty()) {
                    Toast.makeText(requireContext(), "Ingresa un email", Toast.LENGTH_SHORT).show()
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
                    Toast.makeText(requireContext(), "Email inválido", Toast.LENGTH_SHORT).show()
                } else {
                    val sharedPref = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("email", nuevoEmail)
                        apply()
                    }

                    Toast.makeText(requireContext(), "Email actualizado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            btnCancelar?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
