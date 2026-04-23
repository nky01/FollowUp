package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.content.Context
// Permite acceder a recursos del sistema

import android.content.Intent
// Navegación entre pantallas

import android.content.SharedPreferences
// Almacenamiento local clave-valor

import android.net.Uri
// Manejo de URIs (imagen de perfil)

import android.os.Bundle
// Contenedor de datos del Fragment

import androidx.fragment.app.Fragment
// Clase base para Fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ImageView
import android.widget.TextView

import androidx.lifecycle.lifecycleScope
// Corrutinas ligadas al ciclo de vida

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.followup.R
// Recursos del proyecto

import com.followup.data.adapter.SeguimientoHomeAdapter
// Adapter para listas de ventas/seguimientos

import com.followup.data.database.AppDatabase
// Base de datos ROOM

import com.followup.data.entity.Venta
// Entidad Venta

import com.followup.presentation.settings.Configuracion
// Pantalla de configuración

import com.followup.presentation.settings.SessionManager
// Manejo de sesión

import de.hdodenhof.circleimageview.CircleImageView
// Imagen circular

import kotlinx.coroutines.launch
// Corrutinas

import java.util.Calendar
// Manejo de fechas

/* ----------------------------------------------------------------------------------------
                                      FRAGMENT INICIO
---------------------------------------------------------------------------------------- */
/*
    [+] - Pantalla principal (Home)
    [+] - Muestra:
        - Saludo al usuario
        - Seguimientos pendientes
        - Ventas recientes
        - Métricas generales
*/

class InicioFragment : Fragment() {

    /* ----------------------------------------------------------------------------------------
                                           ATRIBUTOS
    ---------------------------------------------------------------------------------------- */

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sessionManager: SessionManager

    /* ---------- RecyclerViews ---------- */

    private lateinit var rvSeguimientos: RecyclerView
    private lateinit var rvVentasRecientes: RecyclerView

    private lateinit var seguimientosAdapter: SeguimientoHomeAdapter
    private lateinit var ventasAdapter: SeguimientoHomeAdapter

    /* ---------- Estado de listas ---------- */

    private var listaCompletaSeguimientos: List<Venta> = emptyList()
    private var listaCompletaVentas: List<Venta> = emptyList()

    private var expandidoSeguimientos = false
    private var expandidoVentas = false

    /* ----------------------------------------------------------------------------------------
                                      MÉTODOS PREDEFINIDOS
    ---------------------------------------------------------------------------------------- */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        initServices()
        initComponents(view)
        setupRecyclerViews()
        initListeners(view)

        return view
    }

    override fun onResume() {
        super.onResume()

        actualizarSaludo()
        cargarDatos()
    }

    /* ----------------------------------------------------------------------------------------
                                      INICIALIZACIÓN
    ---------------------------------------------------------------------------------------- */

    private fun initServices() {

        sessionManager = SessionManager(requireContext())

        sharedPreferences = requireActivity()
            .getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
    }

    private fun initComponents(view: View) {

        rvSeguimientos = view.findViewById(R.id.rv_seguimientos)
        rvVentasRecientes = view.findViewById(R.id.rv_ventas_recientes)

        configurarImagenPerfil(view)
        actualizarSaludo()
    }

    /* ----------------------------------------------------------------------------------------
                                      CONFIGURACIONES UI
    ---------------------------------------------------------------------------------------- */

    private fun configurarImagenPerfil(view: View) {

        val prefs = requireContext()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)

        val ivProfile = view.findViewById<CircleImageView>(R.id.iv_profile_picture)

        val uriString = prefs.getString("profile_image_uri", null)

        uriString?.let {
            try {
                ivProfile.setImageURI(Uri.parse(it))
            } catch (_: Exception) {
                ivProfile.setImageResource(android.R.color.darker_gray)
            }
        }

        ivProfile.setOnClickListener {
            startActivity(Intent(requireContext(), Configuracion::class.java))
        }
    }

    private fun setupRecyclerViews() {

        seguimientosAdapter = SeguimientoHomeAdapter(false)
        rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
        rvSeguimientos.adapter = seguimientosAdapter

        ventasAdapter = SeguimientoHomeAdapter(true)
        rvVentasRecientes.layoutManager = LinearLayoutManager(requireContext())
        rvVentasRecientes.adapter = ventasAdapter
    }

    /* ----------------------------------------------------------------------------------------
                                      LISTENERS (EVENTOS)
    ---------------------------------------------------------------------------------------- */

    private fun initListeners(view: View) {

        setupNavegacionRapida(view)
        setupToggles(view)
    }

    private fun setupNavegacionRapida(view: View) {

        val bottomNav = requireActivity()
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottom_navigation
            )

        view.findViewById<View>(R.id.card_clientes)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Clientes
        }

        view.findViewById<View>(R.id.card_ventas)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.bottom_Ventas
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      TOGGLES (VER MÁS / MENOS)
    ---------------------------------------------------------------------------------------- */

    private fun setupToggles(view: View) {

        val tvSeg = view.findViewById<TextView>(R.id.tvToggle_Seguimientos)
        val ivSeg = view.findViewById<ImageView>(R.id.ivToggle_Seguimientos)

        val tvVen = view.findViewById<TextView>(R.id.tvToggle_VentasRec)
        val ivVen = view.findViewById<ImageView>(R.id.ivToggle_VentasRec)

        val toggleSeguimientos = {
            expandidoSeguimientos = !expandidoSeguimientos
            actualizarListaSeguimientos()
            animarToggle(ivSeg, tvSeg, expandidoSeguimientos)
        }

        val toggleVentas = {
            expandidoVentas = !expandidoVentas
            actualizarListaVentas()
            animarToggle(ivVen, tvVen, expandidoVentas)
        }

        tvSeg.setOnClickListener { toggleSeguimientos() }
        ivSeg.setOnClickListener { toggleSeguimientos() }

        tvVen.setOnClickListener { toggleVentas() }
        ivVen.setOnClickListener { toggleVentas() }
    }

    private fun animarToggle(iv: ImageView, tv: TextView, expandido: Boolean) {
        iv.animate().rotation(if (expandido) 90f else 0f).setDuration(200).start()
        tv.text = if (expandido) "Ver menos" else "Ver todos"
    }

    /* ----------------------------------------------------------------------------------------
                                      LÓGICA DE UI
    ---------------------------------------------------------------------------------------- */

    private fun actualizarSaludo() {

        val userName = sharedPreferences.getString("USER_NAME", "Usuario")

        view?.findViewById<TextView>(R.id.tv_saludo)?.text =
            "Hola, $userName"
    }

    /* ----------------------------------------------------------------------------------------
                                      CARGA DE DATOS
    ---------------------------------------------------------------------------------------- */

    private fun cargarDatos() {

        lifecycleScope.launch {

            try {

                val db = AppDatabase.getDatabase(requireContext())
                val userMail = sessionManager.getUserMail()

                val ventas = db.ventaDao().obtenerTodas(userMail)

                actualizarMetricas(db, ventas, userMail)

                procesarSeguimientos(ventas)
                procesarVentas(ventas)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      MÉTRICAS
    ---------------------------------------------------------------------------------------- */

    private suspend fun actualizarMetricas(
        db: AppDatabase,
        ventas: List<Venta>,
        userMail: String
    ) {

        val clientesCount = db.clienteDao()
            .obtenerTodos(userMail)
            .size

        val ventasCount = ventas.size

        val hoy = obtenerInicioDelDia()

        val alertasCount = ventas.count {
            it.estado == "Pendiente" &&
                    it.fechaSeguimiento >= it.fechaVenta &&
                    it.fechaSeguimiento >= hoy
        }

        view?.let {
            it.findViewById<TextView>(R.id.tv_count_clientes).text = clientesCount.toString()
            it.findViewById<TextView>(R.id.tv_count_ventas).text = ventasCount.toString()
            it.findViewById<TextView>(R.id.tv_count_alertas).text = alertasCount.toString()
        }
    }

    /* ----------------------------------------------------------------------------------------
                                      PROCESAMIENTO DE LISTAS
    ---------------------------------------------------------------------------------------- */

    private fun procesarSeguimientos(ventas: List<Venta>) {

        val hoy = obtenerInicioDelDia()

        listaCompletaSeguimientos = ventas
            .filter {
                it.estado == "Pendiente" &&
                        it.fechaSeguimiento >= it.fechaVenta &&
                        it.fechaSeguimiento >= hoy
            }
            .sortedBy { it.fechaSeguimiento }

        actualizarListaSeguimientos()
    }

    private fun procesarVentas(ventas: List<Venta>) {

        listaCompletaVentas = ventas
            .filter { it.estado == "Pagado" }
            .sortedByDescending { it.fechaVenta }

        actualizarListaVentas()
    }

    private fun actualizarListaSeguimientos() {

        val lista = if (expandidoSeguimientos)
            listaCompletaSeguimientos
        else
            listaCompletaSeguimientos.take(3)

        seguimientosAdapter.submitList(lista)
    }

    private fun actualizarListaVentas() {

        val lista = if (expandidoVentas)
            listaCompletaVentas
        else
            listaCompletaVentas.take(3)

        ventasAdapter.submitList(lista)
    }

    /* ----------------------------------------------------------------------------------------
                                      UTILIDADES
    ---------------------------------------------------------------------------------------- */

    private fun obtenerInicioDelDia(): Long {

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}