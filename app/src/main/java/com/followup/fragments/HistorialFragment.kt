package com.followup.fragments

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
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HistorialFragment : Fragment() {

    private lateinit var adapter: HistorialAdapter
    private var listaCompleta = mutableListOf<HistorialItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_historial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupFiltros(view)
        cargarDatos()
    }

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.historial)
        adapter = HistorialAdapter(
            onRestaurarCliente = { cliente -> restaurarCliente(cliente) },
            onRestaurarVenta = { venta -> restaurarVenta(venta) }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            val clientesEliminados = db.clienteDao().getClientesEnPapelera()
            val ventasEliminadas = db.ventaDao().getVentasEliminadas()

            listaCompleta.clear()
            listaCompleta.addAll(clientesEliminados.map { HistorialItem.ClienteItem(it) })
            listaCompleta.addAll(ventasEliminadas.map { HistorialItem.VentaItem(it) })

            adapter.submitList(listaCompleta.toList())
        }
    }

    private fun setupFiltros(view: View) {
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.filter_group)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
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
    }

    private fun restaurarCliente(cliente: Cliente) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.clienteDao().update(cliente.copy(isDeleted = false))
            cargarDatos()
            Toast.makeText(requireContext(), "${cliente.nombre} restaurado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restaurarVenta(venta: Venta) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.ventaDao().update(venta.copy(isDeleted = false))
            cargarDatos()
            Toast.makeText(requireContext(), "Venta restaurada", Toast.LENGTH_SHORT).show()
        }
    }
}