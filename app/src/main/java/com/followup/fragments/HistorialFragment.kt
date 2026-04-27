package com.followup.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.EstadoCliente
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class HistorialFragment : Fragment() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var pillTodos: MaterialCardView
    private lateinit var pillClientes: MaterialCardView
    private lateinit var pillVentas: MaterialCardView
    private lateinit var adapter: HistorialAdapter
    private var listaCompleta = mutableListOf<HistorialItem>()
    private var filtroActivo: String? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_historial, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        initComponents(view)
        setupRecyclerView()
        initListeners()
        cargarDatos()
        seleccionarPill(pillTodos)
    }

    private fun initComponents(view: View) {
        rvHistorial  = view.findViewById(R.id.historial)
        etSearch     = view.findViewById(R.id.search)
        pillTodos    = view.findViewById(R.id.btn_filter_todos)
        pillClientes = view.findViewById(R.id.btn_filter_clientes)
        pillVentas   = view.findViewById(R.id.btn_filter_ventas)
    }

    private fun setupRecyclerView() {
        rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistorialAdapter(
            onRestaurar      = { item -> confirmarRestaurar(item) },
            onEliminarFisico = { item -> confirmarEliminarFisico(item) }
        )
        rvHistorial.adapter = adapter
    }

    private fun initListeners() {
        pillTodos.setOnClickListener {
            filtroActivo = null
            seleccionarPill(pillTodos)
            aplicarFiltrosYBusqueda()
        }
        pillClientes.setOnClickListener {
            filtroActivo = "cliente"
            seleccionarPill(pillClientes)
            aplicarFiltrosYBusqueda()
        }
        pillVentas.setOnClickListener {
            filtroActivo = "venta"
            seleccionarPill(pillVentas)
            aplicarFiltrosYBusqueda()
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { aplicarFiltrosYBusqueda() }
        })
    }

    private fun seleccionarPill(pillActiva: MaterialCardView) {
        val pills = listOf(pillTodos, pillClientes, pillVentas)
        pills.forEach { pill ->
            val tvId = when (pill.id) {
                R.id.btn_filter_todos    -> R.id.tv_filter_todos
                R.id.btn_filter_clientes -> R.id.tv_filter_clientes
                R.id.btn_filter_ventas   -> R.id.tv_filter_ventas
                else -> return@forEach
            }
            val tv = pill.findViewById<android.widget.TextView>(tvId)

            if (pill.id == pillActiva.id) {
                // Activa — superficie clara para que resalte
                pill.setCardBackgroundColor(
                    requireContext().getColor(R.color.surface_secondary)
                )
                tv.setTextColor(requireContext().getColor(R.color.primary_blue))
            } else {
                // Inactiva — semitransparente sobre el header
                pill.setCardBackgroundColor(
                    requireContext().getColor(R.color.primary_blue_disabled)
                )
                tv.setTextColor(requireContext().getColor(R.color.always_white))
            }
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val db       = AppDatabase.getDatabase(requireContext())
            val userMail = sessionManager.getUserMail()
            combine(
                db.clienteDao().getClientesEnPapelera(userMail),
                db.ventaDao().getVentasEliminadas(userMail)
            ) { clientes, ventas ->
                val items = mutableListOf<HistorialItem>()
                items.addAll(clientes.map { HistorialItem.ClienteItem(it) })
                items.addAll(ventas.map { HistorialItem.VentaItem(it) })
                items.sortedByDescending { item ->
                    when (item) {
                        is HistorialItem.ClienteItem -> item.cliente.fecha
                        is HistorialItem.VentaItem   -> item.venta.fechaVenta
                    }
                }
            }.collect { lista ->
                listaCompleta = lista.toMutableList()
                aplicarFiltrosYBusqueda()
            }
        }
    }

    private fun aplicarFiltrosYBusqueda() {
        val query = etSearch.text.toString().lowercase().trim()
        var listaFiltrada: List<HistorialItem> = when (filtroActivo) {
            "cliente" -> listaCompleta.filterIsInstance<HistorialItem.ClienteItem>()
            "venta"   -> listaCompleta.filterIsInstance<HistorialItem.VentaItem>()
            else      -> listaCompleta
        }
        if (query.isNotEmpty()) {
            listaFiltrada = listaFiltrada.filter { item ->
                when (item) {
                    is HistorialItem.ClienteItem ->
                        item.cliente.nombre.lowercase().contains(query) ||
                                item.cliente.email.lowercase().contains(query)  ||
                                item.cliente.telefono.contains(query)
                    is HistorialItem.VentaItem ->
                        item.venta.nombreCliente.lowercase().contains(query) ||
                                item.venta.descripcion.lowercase().contains(query)
                }
            }
        }
        adapter.submitList(listaFiltrada)
    }

    private fun confirmarRestaurar(item: HistorialItem) {
        val nombre = when (item) {
            is HistorialItem.ClienteItem -> item.cliente.nombre
            is HistorialItem.VentaItem   -> "Venta #${item.venta.id}"
        }
        AlertDialog.Builder(requireContext(), R.style.Dialog_FollowUp)
            .setTitle("Restaurar")
            .setMessage("¿Restaurar $nombre?")
            .setPositiveButton("Restaurar") { _, _ ->
                lifecycleScope.launch {
                    when (item) {
                        is HistorialItem.ClienteItem -> restaurarCliente(item.cliente)
                        is HistorialItem.VentaItem   -> restaurarVenta(item.venta)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminarFisico(item: HistorialItem) {
        val nombre = when (item) {
            is HistorialItem.ClienteItem -> item.cliente.nombre
            is HistorialItem.VentaItem   -> "Venta #${item.venta.id}"
        }
        AlertDialog.Builder(requireContext(), R.style.Dialog_FollowUp)
            .setTitle("Eliminar permanentemente")
            .setMessage("Esta acción no se puede deshacer. ¿Eliminar $nombre de forma permanente?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    when (item) {
                        is HistorialItem.ClienteItem -> eliminarClienteFisico(item.cliente)
                        is HistorialItem.VentaItem   -> eliminarVentaFisica(item.venta)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun restaurarCliente(cliente: Cliente) {
        val db       = AppDatabase.getDatabase(requireContext())
        val userMail = sessionManager.getUserMail()
        db.clienteDao().update(cliente.copy(isDeleted = false))
        recalcularEstadoCliente(cliente.id, userMail)
        Toast.makeText(requireContext(), "${cliente.nombre} restaurado", Toast.LENGTH_SHORT).show()
    }

    private suspend fun restaurarVenta(venta: Venta) {
        val db       = AppDatabase.getDatabase(requireContext())
        val userMail = sessionManager.getUserMail()
        db.ventaDao().update(venta.copy(isDeleted = false))
        recalcularEstadoCliente(venta.idClienteVenta, userMail)
        Toast.makeText(requireContext(), "Venta restaurada", Toast.LENGTH_SHORT).show()
    }

    private suspend fun eliminarClienteFisico(cliente: Cliente) {
        val db = AppDatabase.getDatabase(requireContext())
        db.ventaDao().eliminarVentasPorCliente(cliente.id, sessionManager.getUserMail())
        db.clienteDao().eliminarFisico(cliente.id)
        Toast.makeText(requireContext(), "${cliente.nombre} eliminado permanentemente", Toast.LENGTH_SHORT).show()
    }

    private suspend fun eliminarVentaFisica(venta: Venta) {
        val db       = AppDatabase.getDatabase(requireContext())
        val userMail = sessionManager.getUserMail()
        db.ventaDao().eliminarFisico(venta.id)
        recalcularEstadoCliente(venta.idClienteVenta, userMail)
        Toast.makeText(requireContext(), "Venta eliminada permanentemente", Toast.LENGTH_SHORT).show()
    }

    private suspend fun recalcularEstadoCliente(clienteId: Int, userMail: String) {
        val db      = AppDatabase.getDatabase(requireContext())
        val dao     = db.clienteDao()
        val cliente = dao.obtenerPorId(clienteId) ?: return

        // ✅ Usar inicio del día — caducado solo si fechaSeguimiento < hoy (no el mismo día)
        val inicioDehoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.ventaDao().marcarVentasCaducadas(clienteId, userMail, inicioDehoy)

        val caducadas  = dao.contarVentasCaducadas(clienteId, userMail)
        val pendientes = dao.contarVentasPendientes(clienteId, userMail)
        val pagadas    = dao.contarVentasPagadas(clienteId, userMail)

        if (caducadas == 0 && pendientes == 0 && pagadas == 0) {
            dao.update(cliente.copy(estado = EstadoCliente.NO_ASIGNADO, fechaCambioEstado = null))
            return
        }

        val ahora = System.currentTimeMillis()
        val (nuevoEstado, fechaCambio) = when {
            caducadas  > 0 -> EstadoCliente.PAGO_CADUCADO  to null
            pendientes > 0 -> EstadoCliente.PAGO_PENDIENTE to null
            else           -> EstadoCliente.PAGO_REALIZADO to ahora
        }

        if (cliente.estado == nuevoEstado && cliente.fechaCambioEstado == fechaCambio) return

        dao.update(cliente.copy(estado = nuevoEstado, fechaCambioEstado = fechaCambio))
    }
}