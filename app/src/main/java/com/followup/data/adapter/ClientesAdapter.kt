package com.followup.data.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Cliente
import com.followup.data.entity.EstadoCliente
import com.google.android.material.card.MaterialCardView
import android.content.Intent
import android.net.Uri

/**
 * Adapter del RecyclerView de clientes.
 *
 * Recibe una lista de [Triple] (cliente, ventasPagadas, ventasPendientes)
 * para poder mostrar los contadores sin queries extra desde el ViewHolder.
 *
 * Los colores del borde de la card y del fondo del estado se asignan
 * automáticamente según [EstadoCliente].
 */
class ClientesAdapter(
    private val listener: OnClienteClickListener
) : ListAdapter<Triple<Cliente, Int, Int>, ClientesAdapter.ClienteViewHolder>(DiffCallback()) {

    /* ========================================================================================
                                        INTERFAZ DE CLICKS
       ======================================================================================== */

    interface OnClienteClickListener {
        fun onEditClick(cliente: Cliente)
        fun onDeleteClick(cliente: Cliente)
        fun onDetalleClick(cliente: Cliente)   // abre el dialog de detalle
    }

    /* ========================================================================================
                                        COLORES POR ESTADO
       ========================================================================================
       Centralizado acá para no repetirlo en cada bind.
    */
    private object ColoresEstado {
        const val AZUL    = "#286DFF"   // Nuevo Cliente
        const val NARANJA = "#F79009"   // Pago Pendiente
        const val VERDE   = "#12B76A"   // Pago Realizado
        const val GRIS    = "#98A2B3"   // No Asignado
    }

    /** Devuelve el color hex correspondiente al estado del cliente. */
    private fun colorParaEstado(estado: String): String = when (estado) {
        EstadoCliente.NUEVO_CLIENTE  -> ColoresEstado.AZUL
        EstadoCliente.PAGO_PENDIENTE -> ColoresEstado.NARANJA
        EstadoCliente.PAGO_REALIZADO -> ColoresEstado.VERDE
        else                         -> ColoresEstado.GRIS    // NO_ASIGNADO o desconocido
    }

    /* ========================================================================================
                                        VIEWHOLDER
       ======================================================================================== */

    inner class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val card: MaterialCardView = itemView.findViewById(R.id.cardCliente)
        private val tvNombre: TextView     = itemView.findViewById(R.id.tvNombre)
        private val tvEstado: TextView     = itemView.findViewById(R.id.tvEstado)
        private val tvPagadas: TextView    = itemView.findViewById(R.id.tvVentasPagadas)
        private val tvPendientes: TextView = itemView.findViewById(R.id.tvVentasPendientes)
        private val btnDetalle: MaterialCardView = itemView.findViewById(R.id.btnVerDetalle)

        fun bind(cliente: Cliente, ventasPagadas: Int, ventasPendientes: Int) {

            // Nombre + apellido
            tvNombre.text = "${cliente.nombre} ${cliente.apellido}".trim()

            // Estado
            tvEstado.text = cliente.estado

            // Contadores
            tvPagadas.text    = ventasPagadas.toString()
            tvPendientes.text = ventasPendientes.toString()

            // Color dinámico según estado
            val color = Color.parseColor(colorParaEstado(cliente.estado))
            card.strokeColor = color
            tvEstado.backgroundTintList = android.content.res.ColorStateList.valueOf(color)

            // Botón de detalle → abre el dialog
            btnDetalle.setOnClickListener { listener.onDetalleClick(cliente) }
        }
    }

    /* ========================================================================================
                                    MÉTODOS DEL ADAPTER
       ======================================================================================== */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val (cliente, pagadas, pendientes) = getItem(position)
        holder.bind(cliente, pagadas, pendientes)
    }

    /**
     * Método especial para cargar la lista ya con los contadores.
     * Lo llama el Fragment después de hacer las queries de conteo.
     */
    fun submitListConContadores(data: List<Triple<Cliente, Int, Int>>) {
        submitList(data)
    }

    /* ========================================================================================
                                        DIFFCALLBACK
       ======================================================================================== */

    class DiffCallback : DiffUtil.ItemCallback<Triple<Cliente, Int, Int>>() {
        override fun areItemsTheSame(
            oldItem: Triple<Cliente, Int, Int>,
            newItem: Triple<Cliente, Int, Int>
        ) = oldItem.first.id == newItem.first.id

        override fun areContentsTheSame(
            oldItem: Triple<Cliente, Int, Int>,
            newItem: Triple<Cliente, Int, Int>
        ) = oldItem == newItem
    }
}