package com.followup.fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.followup.R
import de.hdodenhof.circleimageview.CircleImageView

// sharedPreferences para mostrar el nombre del usuario en el saludo, es una alternativa que encontre a ViewModel.
// Seria como un localStorage de Android
class InicioFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        val saludoTextView = view.findViewById<TextView>(R.id.tv_saludo)
        saludoTextView.text = "Hola, $userName"

        // Cargar imagen de perfil persistida
        val userDataPrefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val savedUriString = userDataPrefs.getString("profile_image_uri", null)
        val ivProfilePicture = view.findViewById<CircleImageView>(R.id.iv_profile_picture)
        
        if (savedUriString != null && ivProfilePicture != null) {
            try {
                ivProfilePicture.setImageURI(Uri.parse(savedUriString))
            } catch (e: Exception) {
                ivProfilePicture.setImageResource(android.R.color.darker_gray)
            }
        }

        return view
    }
}
