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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.SeguimientoHomeAdapter
import com.followup.data.database.AppDatabase
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.util.Calendar

class InicioFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvSeguimientos: RecyclerView
    private lateinit var rvVentasRecientes: RecyclerView
    private lateinit var seguimientosAdapter: SeguimientoHomeAdapter
    private lateinit var ventasAdapter: SeguimientoHomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)

        initComponents(view)
        setupRecyclerViews()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initComponents(view: View) {
        rvSeguimientos = view.findViewById(R.id.rv_seguimientos)
        rvVentasRecientes = view.findViewById(R.id.rv_ventas_recientes)

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        view.findViewById<TextView>(R.id.tv_saludo).text = "Hola, $userName"

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


        ivProfilePicture?.setOnClickListener {

            val intent = android.content.Intent(
                requireContext(),
                com.followup.presentation.Perfil::class.java
            )

            startActivity(intent)
        }

        // --- LOGICA DE NAVEGACION DESDE TARJETAS ---

        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        view.findViewById<View>(R.id.card_clientes)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Clientes
        }

        view.findViewById<View>(R.id.card_ventas)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Ventas
        }
    }

    private fun setupRecyclerViews() {
        seguimientosAdapter = SeguimientoHomeAdapter(isVentaReciente = false)
        rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
        rvSeguimientos.adapter = seguimientosAdapter

        ventasAdapter = SeguimientoHomeAdapter(isVentaReciente = true)
        rvVentasRecientes.layoutManager = LinearLayoutManager(requireContext())
        rvVentasRecientes.adapter = ventasAdapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val todasLasVentas = db.ventaDao().obtenerTodas()

                // 1. Obtener conteos para las tarjetas superiores
                val clientesCount = db.clienteDao().obtenerTodos().size
                val ventasCount = todasLasVentas.size

                // Definición de Alerta: Fecha de seguimiento futura Y distinta a la fecha de venta
                // (Normalmente cuando se crea una venta sin seguimiento, ambas fechas coinciden o la de seguimiento es 0)
                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val alertasCount = todasLasVentas.filter {
                    it.fechaSeguimiento > it.fechaVenta && it.fechaSeguimiento >= hoy
                }.size

                view?.let { v ->
                    v.findViewById<TextView>(R.id.tv_count_clientes).text = clientesCount.toString()
                    v.findViewById<TextView>(R.id.tv_count_ventas).text = ventasCount.toString()
                    v.findViewById<TextView>(R.id.tv_count_alertas).text = alertasCount.toString()
                }

                // 2. Cargar Próximos Seguimientos (solo los que son alertas reales)
                val proximosSeguimientos = todasLasVentas
                    .filter { it.fechaSeguimiento > it.fechaVenta && it.fechaSeguimiento >= hoy }
                    .sortedBy { it.fechaSeguimiento }
                    .take(3)

                seguimientosAdapter.submitList(proximosSeguimientos)

                // 3. Cargar Ventas Recientes
                val ventasRecientes = todasLasVentas
                    .sortedByDescending { it.fechaVenta }
                    .take(3)

                ventasAdapter.submitList(ventasRecientes)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
