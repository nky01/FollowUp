package com.followup.data.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Venta
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.followup.ui.EstadoColorHelper
import com.google.android.material.card.MaterialCardView

class VentasAdapter(
    private val listener: OnVentaClickListener
) : ListAdapter<Venta, VentasAdapter.VentaViewHolder>(DiffCallback()) {

    interface OnVentaClickListener {
        fun onDeleteClick(venta: Venta)
        fun onEditClick(venta: Venta)
        fun onDetalleClick(venta: Venta)
    }

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))

    private fun colorParaEstado(estado: String): String = when (estado.lowercase()) {
        "pendiente"      -> "#F79009"
        "pagado"         -> "#12B76A"
        "pago caducado"  -> "#F04438"
        else             -> "#98A2B3"
    }

    inner class VentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val card: MaterialCardView      = itemView.findViewById(R.id.cardVenta)
        private val tvId: TextView              = itemView.findViewById(R.id.tvVentaId)
        private val tvCliente: TextView         = itemView.findViewById(R.id.tvClienteId)
        private val tvEstado: TextView          = itemView.findViewById(R.id.tvEstadoVenta)
        private val tvMonto: TextView           = itemView.findViewById(R.id.tvMonto)
        private val progressPago: ProgressBar   = itemView.findViewById(R.id.progressPago)
        private val tvFecha: TextView           = itemView.findViewById(R.id.tvFecha)
        private val btnDetalle: MaterialCardView = itemView.findViewById(R.id.btnVerDetalleVenta)

        fun bind(venta: Venta) {
            tvId.text      = "Venta #${venta.id}"
            tvCliente.text = venta.nombreCliente
            tvEstado.text  = venta.estado
            tvMonto.text   = "$${DecimalFormat("#,##0.00").format(venta.montoTotal)}"
            tvFecha.text   = "Seg: ${dateFormatter.format(Date(venta.fechaSeguimiento))}"

            val porcentaje = ((venta.pagoTotal / venta.montoTotal) * 100).toInt().coerceIn(0, 100)
            progressPago.progress = porcentaje

            EstadoColorHelper.aplicarBadgeVenta(itemView.context, tvEstado, venta.estado)
            card.strokeColor = ContextCompat.getColor(itemView.context, EstadoColorHelper.badgeBgColorResVenta(venta.estado))

            btnDetalle.setOnClickListener { listener.onDetalleClick(venta) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Venta>() {
        override fun areItemsTheSame(old: Venta, new: Venta) = old.id == new.id
        override fun areContentsTheSame(old: Venta, new: Venta) = old == new
    }
}