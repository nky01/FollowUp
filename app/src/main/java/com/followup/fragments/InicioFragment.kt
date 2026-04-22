package com.followup.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.SeguimientoHomeAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Venta
import com.followup.presentation.Perfil
import com.followup.presentation.settings.Configuracion
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.util.Calendar

class InicioFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvSeguimientos: RecyclerView
    private lateinit var rvVentasRecientes: RecyclerView
    private lateinit var seguimientosAdapter: SeguimientoHomeAdapter
    private lateinit var ventasAdapter: SeguimientoHomeAdapter

    private var listaCompletaSeguimientos: List<Venta> = emptyList()
    private var expandidoSeguimientos = false

    private var listaCompletaVentas: List<Venta> = emptyList()
    private var expandidoVentas = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        sharedPreferences = requireActivity()
            .getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)

        initComponents(view)
        setupRecyclerViews()

        return view
    }

    override fun onResume() {
        super.onResume()
        actualizarSaludo()
        loadData()
    }

    private fun initComponents(view: View) {
        rvSeguimientos = view.findViewById(R.id.rv_seguimientos)
        rvVentasRecientes = view.findViewById(R.id.rv_ventas_recientes)

        actualizarSaludo()

        val userDataPrefs = requireContext()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)

        val savedUriString = userDataPrefs.getString("profile_image_uri", null)
        val ivProfilePicture = view.findViewById<CircleImageView>(R.id.iv_profile_picture)

        savedUriString?.let {
            try {
                ivProfilePicture.setImageURI(Uri.parse(it))
            } catch (_: Exception) {
                ivProfilePicture.setImageResource(android.R.color.darker_gray)
            }
        }

        // Navegación a perfil/configuración
        ivProfilePicture.setOnClickListener {
            startActivity(Intent(requireContext(), Perfil::class.java))
        }

        val bottomNav = requireActivity()
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        view.findViewById<View>(R.id.card_clientes)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Clientes
        }

        view.findViewById<View>(R.id.card_ventas)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Ventas
        }

        setupToggles(view)
    }

    private fun actualizarSaludo() {
        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        view?.findViewById<TextView>(R.id.tv_saludo)?.text = "Hola, $userName"
    }

    private fun setupToggles(view: View) {
        val tvToggle = view.findViewById<TextView>(R.id.tvToggle_Seguimientos)
        val ivToggle = view.findViewById<ImageView>(R.id.ivToggle_Seguimientos)

        val tvToggleVentas = view.findViewById<TextView>(R.id.tvToggle_VentasRec)
        val ivToggleVentas = view.findViewById<ImageView>(R.id.ivToggle_VentasRec)

        val toggleSeguimientos = {
            expandidoSeguimientos = !expandidoSeguimientos

            val lista = if (expandidoSeguimientos)
                listaCompletaSeguimientos
            else
                listaCompletaSeguimientos.take(3)

            seguimientosAdapter.submitList(lista)

            ivToggle.animate().rotation(if (expandidoSeguimientos) 90f else 0f).setDuration(200).start()
            tvToggle.text = if (expandidoSeguimientos) "Ver menos" else "Ver todos"
        }

        val toggleVentas = {
            expandidoVentas = !expandidoVentas

            val lista = if (expandidoVentas)
                listaCompletaVentas
            else
                listaCompletaVentas.take(3)

            ventasAdapter.submitList(lista)

            ivToggleVentas.animate().rotation(if (expandidoVentas) 90f else 0f).setDuration(200).start()
            tvToggleVentas.text = if (expandidoVentas) "Ver menos" else "Ver todos"
        }

        tvToggle.setOnClickListener { toggleSeguimientos() }
        ivToggle.setOnClickListener { toggleSeguimientos() }

        tvToggleVentas.setOnClickListener { toggleVentas() }
        ivToggleVentas.setOnClickListener { toggleVentas() }
    }

    private fun setupRecyclerViews() {
        seguimientosAdapter = SeguimientoHomeAdapter(false)
        rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
        rvSeguimientos.adapter = seguimientosAdapter

        ventasAdapter = SeguimientoHomeAdapter(true)
        rvVentasRecientes.layoutManager = LinearLayoutManager(requireContext())
        rvVentasRecientes.adapter = ventasAdapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val todasLasVentas = db.ventaDao().obtenerTodas()

                val clientesCount = db.clienteDao().obtenerTodos().size
                val ventasCount = todasLasVentas.size

                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val alertasCount = todasLasVentas.count {
                    it.estado == "Pendiente" &&
                            it.fechaSeguimiento >= it.fechaVenta &&
                            it.fechaSeguimiento >= hoy
                }

                view?.let {
                    it.findViewById<TextView>(R.id.tv_count_clientes).text = clientesCount.toString()
                    it.findViewById<TextView>(R.id.tv_count_ventas).text = ventasCount.toString()
                    it.findViewById<TextView>(R.id.tv_count_alertas).text = alertasCount.toString()
                }

                listaCompletaSeguimientos = todasLasVentas
                    .filter {
                        it.estado == "Pendiente" &&
                                it.fechaSeguimiento >= it.fechaVenta &&
                                it.fechaSeguimiento >= hoy
                    }
                    .sortedBy { it.fechaSeguimiento }

                val listaSeg = if (expandidoSeguimientos)
                    listaCompletaSeguimientos
                else
                    listaCompletaSeguimientos.take(3)

                seguimientosAdapter.submitList(listaSeg)

                listaCompletaVentas = todasLasVentas
                    .filter { it.estado == "Pagado" }
                    .sortedByDescending { it.fechaVenta }

                val listaVen = if (expandidoVentas)
                    listaCompletaVentas
                else
                    listaCompletaVentas.take(3)

                ventasAdapter.submitList(listaVen)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}