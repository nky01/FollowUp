package com.followup.data.adapter

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

class VentasAdapter(
    private val listener: OnVentaClickListener
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    interface OnVentaClickListener {
        fun onDeleteClick(venta: Venta)
        fun onEditClick(venta: Venta)
    }

    private val items = mutableListOf<Venta>()
    private val dateFormatter = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-AR"))
    private val moneyFormatter = DecimalFormat("#,##0.00")

    fun submitList(ventas: List<Venta>) {
        items.clear()
        items.addAll(ventas)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        holder.bind(items[position], dateFormatter, moneyFormatter)
    }

    override fun getItemCount(): Int = items.size

    class VentaViewHolder(
        itemView: View,
        private val listener: OnVentaClickListener
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvVentaId: TextView = itemView.findViewById(R.id.tvVentaId)
        private val tvCliente: TextView = itemView.findViewById(R.id.tvClienteId)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoVenta)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvMonto: TextView = itemView.findViewById(R.id.tvMonto)
        private val tvSeguimiento: TextView = itemView.findViewById(R.id.tvSeguimiento)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescVenta)
        private val progressPago: ProgressBar = itemView.findViewById(R.id.progressPago)

        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEditVenta)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDeleteVenta)

        private val ivToggle: ImageView = itemView.findViewById(R.id.ivToggle)
        private val layoutMontoVenta: LinearLayout = itemView.findViewById(R.id.layoutMontoVenta)

        fun bind(venta: Venta, formatter: SimpleDateFormat, moneyFormatter: DecimalFormat) {

            tvVentaId.text = "Venta #${venta.id}"
            tvCliente.text = venta.nombreCliente
            tvEstado.text = venta.estado
            tvFecha.text = formatter.format(Date(venta.fechaVenta))

            layoutMontoVenta.visibility = View.GONE
            ivToggle.rotation = 0f

            tvMonto.text = "$${moneyFormatter.format(venta.pagoTotal)} / ${moneyFormatter.format(venta.montoTotal)}"
            tvSeguimiento.text = "Seguimiento: ${formatter.format(Date(venta.fechaSeguimiento))}"
            tvDescripcion.text = if (venta.descripcion.isBlank()) "[Sin descripción]" else venta.descripcion

            // PROGRESS BAR
            val porcentaje = ((venta.pagoTotal.toFloat() / venta.montoTotal) * 100).toInt()
            progressPago.progress = porcentaje

            // COLOR ESTADO
            val colors = estadoColors(venta.estado)
            tvEstado.setBackgroundColor(colors.first)
            tvEstado.setTextColor(colors.second)

            // BOTONES
            ivEdit.setOnClickListener { listener.onEditClick(venta) } // BOTÓN EDITAR
            ivDelete.setOnClickListener { listener.onDeleteClick(venta) } // BOTÓN ELIMINAR

            // OCULTAR / MOSTRAR MAS INFO DE LA VENTA
            ivToggle.setOnClickListener {

                val expanded = layoutMontoVenta.visibility == View.VISIBLE

                if (expanded) {
                    // CERRAR
                    layoutMontoVenta.visibility = View.GONE
                    ivToggle.animate().rotation(0f).setDuration(200).start()
                } else {
                    // ABRIR
                    layoutMontoVenta.visibility = View.VISIBLE
                    ivToggle.animate().rotation(90f).setDuration(200).start()
                }
            }
        }

        private fun estadoColors(estado: String): Pair<Int, Int> {
            return when (estado.lowercase(Locale.ROOT)) {
                "pagado", "vendido" -> Pair(Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"))
                "pendiente" -> Pair(Color.parseColor("#FFF4E5"), Color.parseColor("#D4850D"))
                else -> Pair(Color.parseColor("#F2F4F7"), Color.parseColor("#475467"))
            }
        }
    }

}
