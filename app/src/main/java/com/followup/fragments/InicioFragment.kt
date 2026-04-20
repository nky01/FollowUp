package com.followup.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.followup.R
import com.followup.presentation.settings.Configuracion
import de.hdodenhof.circleimageview.CircleImageView

class InicioFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        view.findViewById<TextView>(R.id.tv_saludo).text = "Hola, $userName"

        val profilePicture = view.findViewById<CircleImageView>(R.id.iv_profile_picture)
        profilePicture.setOnClickListener {
            startActivity(Intent(requireContext(), Configuracion::class.java))
        }

        return view
    }

    // Refresh name when returning from Configuracion
    override fun onResume() {
        super.onResume()
        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        view?.findViewById<TextView>(R.id.tv_saludo)?.text = "Hola, $userName"
    }
}