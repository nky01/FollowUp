package com.followup.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Cliente
import com.followup.data.entity.EstadoCliente
import com.followup.data.entity.Venta
import com.followup.ui.EstadoColorHelper
import com.google.android.material.card.MaterialCardView

/* ========================================================================================
                                    MODELO DE ITEM
   ======================================================================================== */

sealed class HistorialItem {
    data class ClienteItem(val cliente: Cliente) : HistorialItem()
    data class VentaItem(val venta: Venta) : HistorialItem()
}

/* ========================================================================================
                                    ADAPTER
   ======================================================================================== */

/**
 * Adapter del historial (papelera).
 * Muestra clientes y ventas eliminadas en una misma lista.
 * Ambos tipos usan el mismo layout [R.layout.item_historial].
 *
 * @param onRestaurar      Se ejecuta al presionar el botón de restaurar.
 * @param onEliminarFisico Se ejecuta al presionar el botón de eliminar permanente.
 */
class HistorialAdapter(
    private val onRestaurar: (HistorialItem) -> Unit,
    private val onEliminarFisico: (HistorialItem) -> Unit
) : ListAdapter<HistorialItem, HistorialAdapter.HistorialViewHolder>(DiffCallback()) {

    /* ========================================================================================
                                        VIEW HOLDER
       ======================================================================================== */

    inner class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Referencias a las vistas del item_historial.xml
        private val iconContainer: MaterialCardView = itemView.findViewById(R.id.iv_tipo_icono_container)
        private val ivIcono: ImageView              = itemView.findViewById(R.id.iv_tipo_icono)
        private val tvNombre: TextView              = itemView.findViewById(R.id.tv_nombre_item)
        private val tvDetalle: TextView             = itemView.findViewById(R.id.tv_detalle_secundario)
        private val tvBadge: TextView               = itemView.findViewById(R.id.tv_badge_tipo)
        private val btnRestaurar: MaterialCardView  = itemView.findViewById(R.id.btn_restaurar_item)
        private val btnEliminar: MaterialCardView   = itemView.findViewById(R.id.btn_eliminar_permanente)

        fun bind(item: HistorialItem) {
            when (item) {

                /* --------------------------------------------------
                                    CLIENTE
                -------------------------------------------------- */
                is HistorialItem.ClienteItem -> {
                    val c = item.cliente

                    tvNombre.text  = "${c.nombre} ${c.apellido}".trim()
                    tvDetalle.text = c.email

                    tvBadge.text = "Cliente"
                    EstadoColorHelper.aplicarBadgeCliente(itemView.context, tvBadge, EstadoCliente.NUEVO_CLIENTE)

                    ivIcono.setImageResource(R.drawable.ic_clients)
                    iconContainer.setCardBackgroundColor(Color.parseColor("#EBF1FF"))
                }

                /* --------------------------------------------------
                                    VENTA
                -------------------------------------------------- */
                is HistorialItem.VentaItem -> {
                    val v = item.venta

                    tvNombre.text  = "${v.nombreCliente} — $${v.montoTotal}"
                    tvDetalle.text = v.descripcion.ifEmpty { v.estado }

                    tvBadge.text = "Venta"
                    EstadoColorHelper.aplicarBadgeVenta(itemView.context, tvBadge, "Pendiente")

                    ivIcono.setImageResource(R.drawable.ic_ventas)
                    iconContainer.setCardBackgroundColor(Color.parseColor("#FFF4E5"))
                }
            }

            // Botones — iguales para ambos tipos
            btnRestaurar.setOnClickListener  { onRestaurar(item) }
            btnEliminar.setOnClickListener   { onEliminarFisico(item) }
        }
    }

    /* ========================================================================================
                                    MÉTODOS DEL ADAPTER
       ======================================================================================== */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /* ========================================================================================
                                    DIFF CALLBACK
       ======================================================================================== */

    class DiffCallback : DiffUtil.ItemCallback<HistorialItem>() {

        override fun areItemsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean {
            return when {
                oldItem is HistorialItem.ClienteItem && newItem is HistorialItem.ClienteItem ->
                    oldItem.cliente.id == newItem.cliente.id
                oldItem is HistorialItem.VentaItem && newItem is HistorialItem.VentaItem ->
                    oldItem.venta.id == newItem.venta.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean =
            oldItem == newItem
    }
}