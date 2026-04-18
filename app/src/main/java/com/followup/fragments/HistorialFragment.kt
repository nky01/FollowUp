package com.followup.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.Venta
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class HistorialFragment : Fragment() {
    private lateinit var rvHistorial: RecyclerView
    private lateinit var filterGroup: MaterialButtonToggleGroup

    private lateinit var btnFiltroTodos: MaterialButton
    private lateinit var btnFiltroClientes: MaterialButton
    private lateinit var btnFiltroVentas: MaterialButton

    private lateinit var adapter: HistorialAdapter
    private var listaCompleta = mutableListOf<HistorialItem>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_historial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponents(view)
        setupRecyclerView()
        initListeners()

        cargarDatos()

        filterGroup.check(R.id.btn_filter_todos)
        actualizarEstilosFiltros(R.id.btn_filter_todos)
    }

    private fun initComponents(view: View) {
        rvHistorial = view.findViewById(R.id.historial)
        filterGroup = view.findViewById(R.id.filter_group)

        btnFiltroTodos = view.findViewById(R.id.btn_filter_todos)
        btnFiltroClientes = view.findViewById(R.id.btn_filter_clientes)
        btnFiltroVentas = view.findViewById(R.id.btn_filter_ventas)
    }

    private fun initListeners() {
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            actualizarEstilosFiltros(checkedId)

            when (checkedId) {
                R.id.btn_filter_todos -> adapter.submitList(listaCompleta.toList())
                R.id.btn_filter_clientes -> {
                    adapter.submitList(listaCompleta.filterIsInstance<HistorialItem.ClienteItem>())
                }
                R.id.btn_filter_ventas -> {
                    adapter.submitList(listaCompleta.filterIsInstance<HistorialItem.VentaItem>())
                }
            }
        }
    }

    private fun actualizarEstilosFiltros(selectedId: Int) {
        val botones = listOf(btnFiltroTodos, btnFiltroClientes, btnFiltroVentas)
        for (boton in botones) {
            if (boton.id == selectedId) {
                boton.setBackgroundColor(Color.parseColor("#286DFF"))
                boton.setTextColor(Color.WHITE)
            } else {
                boton.setBackgroundColor(Color.WHITE)
                boton.setTextColor(Color.parseColor("#475467"))
            }
        }
    }

    private fun setupRecyclerView() {
        rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistorialAdapter(
            onRestaurarCliente = { cliente -> restaurarCliente(cliente) },
            onRestaurarVenta = { venta -> restaurarVenta(venta) }
        )
        rvHistorial.adapter = adapter
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            combine(
                db.clienteDao().getClientesEnPapelera().onStart { emit(emptyList()) },
                db.ventaDao().getVentasEliminadas().onStart { emit(emptyList()) }
            ) { clientes, ventas ->
                val items = mutableListOf<HistorialItem>()
                items.addAll(clientes.map { HistorialItem.ClienteItem(it) })
                items.addAll(ventas.map { HistorialItem.VentaItem(it) })

                // Ordenamos por fecha descendente
                items.sortByDescending { item ->
                    when(item) {
                        is HistorialItem.ClienteItem -> item.cliente.fecha
                        is HistorialItem.VentaItem -> item.venta.fechaVenta
                    }
                }
                items
            }.collect { lista ->
                listaCompleta = lista.toMutableList()
                aplicarFiltroActual()
            }
        }
    }

    private fun aplicarFiltroActual() {
        when (filterGroup.checkedButtonId) {
            R.id.btn_filter_clientes -> adapter.submitList(listaCompleta.filterIsInstance<HistorialItem.ClienteItem>())
            R.id.btn_filter_ventas -> adapter.submitList(listaCompleta.filterIsInstance<HistorialItem.VentaItem>())
            else -> adapter.submitList(listaCompleta.toList())
        }
    }

    private fun restaurarCliente(cliente: Cliente) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.clienteDao().update(cliente.copy(isDeleted = false))
            Toast.makeText(requireContext(), "${cliente.nombre} restaurado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restaurarVenta(venta: Venta) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.ventaDao().update(venta.copy(isDeleted = false))
            Toast.makeText(requireContext(), "Venta restaurada", Toast.LENGTH_SHORT).show()
        }
    }
}