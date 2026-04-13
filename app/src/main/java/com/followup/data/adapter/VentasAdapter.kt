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
            .inflate(R.layout.item_cliente, parent, false)
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

        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvSubtitulo: TextView = itemView.findViewById(R.id.tvSubtitulo)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvDescTag: TextView = itemView.findViewById(R.id.tvDescTag)
        private val tvTelefonoValue: TextView = itemView.findViewById(R.id.tvTelefonoValue)
        private val tvEmailValue: TextView = itemView.findViewById(R.id.tvEmailValue)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val ivToggle: ImageView = itemView.findViewById(R.id.ivToggle)
        private val layoutContacto: LinearLayout = itemView.findViewById(R.id.layoutContacto)

        fun bind(venta: Venta, formatter: SimpleDateFormat, moneyFormatter: DecimalFormat) {
            tvNombre.text = "Venta #${venta.id}"
            tvSubtitulo.text = venta.nombreCliente
            tvSubtitulo.visibility = View.VISIBLE
            tvEstado.text = venta.estado
            tvFecha.text = formatter.format(Date(venta.fechaVenta))
            tvDescTag.text = if (venta.descripcion.isBlank()) "Sin descripcion" else venta.descripcion
            tvTelefonoValue.text = "Pago: ${moneyFormatter.format(venta.pagoTotal)} / ${moneyFormatter.format(venta.montoTotal)}"
            tvEmailValue.text = "Seguimiento: ${formatter.format(Date(venta.fechaSeguimiento))}"

            val colors = estadoColors(venta.estado)
            tvEstado.setBackgroundColor(colors.first)
            tvEstado.setTextColor(colors.second)

            ivEdit.setOnClickListener { listener.onEditClick(venta) }
            ivDelete.setOnClickListener { listener.onDeleteClick(venta) }

            layoutContacto.visibility = View.GONE
            ivToggle.rotation = 0f

            ivToggle.setOnClickListener {
                val expanded = layoutContacto.visibility == View.VISIBLE
                layoutContacto.visibility = if (expanded) View.GONE else View.VISIBLE
                ivToggle.animate().rotation(if (expanded) 0f else 90f).setDuration(200).start()
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
