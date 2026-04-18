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
import kotlinx.coroutines.launch
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText

class HistorialFragment : Fragment() {
    private lateinit var rvHistorial: RecyclerView
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var etSearch: TextInputEditText

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
        etSearch = view.findViewById(R.id.search)
        btnFiltroTodos = view.findViewById(R.id.btn_filter_todos)
        btnFiltroClientes = view.findViewById(R.id.btn_filter_clientes)
        btnFiltroVentas = view.findViewById(R.id.btn_filter_ventas)
    }

    private fun setupRecyclerView() {
        rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistorialAdapter(
            onRestaurarCliente = { cliente -> restaurarCliente(cliente) },
            onRestaurarVenta = { venta -> restaurarVenta(venta) }
        )
        rvHistorial.adapter = adapter
    }

    private fun initListeners() {
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                actualizarEstilosFiltros(checkedId)
                aplicarFiltrosYBusqueda()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                aplicarFiltrosYBusqueda()
            }
        })
    }

    private fun aplicarFiltrosYBusqueda() {
        val query = etSearch.text.toString().lowercase().trim()
        val checkedId = filterGroup.checkedButtonId

        var listaFiltrada = when (checkedId) {
            R.id.btn_filter_clientes -> listaCompleta.filterIsInstance<HistorialItem.ClienteItem>()
            R.id.btn_filter_ventas -> listaCompleta.filterIsInstance<HistorialItem.VentaItem>()
            else -> listaCompleta
        }

        if (query.isNotEmpty()) {
            listaFiltrada = listaFiltrada.filter { item ->
                when (item) {
                    is HistorialItem.ClienteItem -> {
                        item.cliente.nombre.lowercase().contains(query) ||
                                item.cliente.email.lowercase().contains(query) ||
                                item.cliente.telefono.contains(query)
                    }
                    is HistorialItem.VentaItem -> {
                        item.venta.nombreCliente.lowercase().contains(query) ||
                                item.venta.formaPago.lowercase().contains(query)
                    }
                    else -> false
                }
            }
        }

        adapter.submitList(listaFiltrada)
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            combine(
                db.clienteDao().getClientesEnPapelera(),
                db.ventaDao().getVentasEliminadas()
            ) { clientes, ventas ->
                val items = mutableListOf<HistorialItem>()
                items.addAll(clientes.map { HistorialItem.ClienteItem(it) })
                items.addAll(ventas.map { HistorialItem.VentaItem(it) })

                items.sortedByDescending { item ->
                    when (item) {
                        is HistorialItem.ClienteItem -> item.cliente.fecha
                        is HistorialItem.VentaItem -> item.venta.fechaVenta
                    }
                }
            }.collect { lista ->
                listaCompleta = lista.toMutableList()
                aplicarFiltrosYBusqueda()
            }
        }
    }

    private fun actualizarEstilosFiltros(selectedId: Int) {
        val colorAzul = Color.parseColor("#286DFF")
        val colorGrisFondo = Color.parseColor("#F0F0F0")
        val colorTextoGris = Color.parseColor("#475467")
        val colorBlanco = Color.WHITE

        val botones = listOf(btnFiltroTodos, btnFiltroClientes, btnFiltroVentas)

        botones.forEach { boton ->
            if (boton.id == selectedId) {
                boton.backgroundTintList = ColorStateList.valueOf(colorAzul)
                boton.setTextColor(colorBlanco)
                boton.elevation = 4f
            } else {
                boton.backgroundTintList = ColorStateList.valueOf(colorGrisFondo)
                boton.setTextColor(colorTextoGris)
                boton.elevation = 0f
            }
        }
    }

    private fun restaurarCliente(cliente: Cliente) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.clienteDao().update(cliente.copy(isDeleted = false))
            Toast.makeText(requireContext(), "${cliente.nombre} restaurado", Toast.LENGTH_SHORT)
                .show()
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