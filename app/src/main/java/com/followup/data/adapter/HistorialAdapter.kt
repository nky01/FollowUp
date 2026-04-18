package com.followup.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Cliente
import com.followup.data.entity.Venta
import com.google.android.material.button.MaterialButton

sealed class HistorialItem {
    data class ClienteItem(val cliente: Cliente) : HistorialItem()
    data class VentaItem(val venta: Venta) : HistorialItem()
}

class HistorialAdapter(
    private val onRestaurarCliente: (Cliente) -> Unit,
    private val onRestaurarVenta: (Venta) -> Unit
) : ListAdapter<HistorialItem, RecyclerView.ViewHolder>(DiffCallback) {

    private val TYPE_CLIENTE = 1
    private val TYPE_VENTA = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistorialItem.ClienteItem -> TYPE_CLIENTE
            is HistorialItem.VentaItem -> TYPE_VENTA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_historial, parent, false)
        return when (viewType) {
            TYPE_CLIENTE -> ClienteViewHolder(view)
            else -> VentaViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistorialItem.ClienteItem -> (holder as ClienteViewHolder).bind(item.cliente)
            is HistorialItem.VentaItem -> (holder as VentaViewHolder).bind(item.venta)
        }
    }

    inner class ClienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre = view.findViewById<TextView>(R.id.tv_nombre_item)
        private val tvDetalle = view.findViewById<TextView>(R.id.tv_detalle_secundario)
        private val btnRestaurar = view.findViewById<MaterialButton>(R.id.btn_restaurar_item)

        fun bind(cliente: Cliente) {
            tvNombre.text = cliente.nombre
            tvDetalle.text = "Cliente | Tel: ${cliente.telefono}"
            btnRestaurar.setOnClickListener { onRestaurarCliente(cliente) }
        }
    }

    inner class VentaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre = view.findViewById<TextView>(R.id.tv_nombre_item)
        private val tvDetalle = view.findViewById<TextView>(R.id.tv_detalle_secundario)
        private val btnRestaurar = view.findViewById<MaterialButton>(R.id.btn_restaurar_item)

        fun bind(venta: Venta) {
            tvNombre.text = "Venta a: ${venta.idClienteVenta}"
            tvDetalle.text = "Monto: $${venta.total}"
            btnRestaurar.setOnClickListener { onRestaurarVenta(venta) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HistorialItem>() {
        override fun areItemsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean {
            return if (oldItem is HistorialItem.ClienteItem && newItem is HistorialItem.ClienteItem) {
                oldItem.cliente.id == newItem.cliente.id
            } else if (oldItem is HistorialItem.VentaItem && newItem is HistorialItem.VentaItem) {
                oldItem.venta.id == newItem.venta.id
            } else false
        }

        override fun areContentsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean {
            return oldItem == newItem
        }
    }
}