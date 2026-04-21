package com.followup.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Venta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeguimientoHomeAdapter(private val isVentaReciente: Boolean = false) :
    ListAdapter<Venta, SeguimientoHomeAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seguimiento_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isVentaReciente)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvCliente: TextView = view.findViewById(R.id.tv_cliente_nombre)
        private val tvProducto: TextView = view.findViewById(R.id.tv_producto_nombre)
        private val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        private val tvHora: TextView = view.findViewById(R.id.tv_hora)
        private val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        private val viewIconBg: View = view.findViewById(R.id.view_icon_bg)

        fun bind(venta: Venta, isVentaReciente: Boolean) {
            tvCliente.text = venta.nombreCliente
            tvProducto.text = venta.descripcion
            
            val sdfFecha = SimpleDateFormat("dd-MMM", Locale("es", "AR"))
            val sdfHora = SimpleDateFormat("HH:mm", Locale("es", "AR"))
            
            // Usamos fechaSeguimiento para seguimientos y fechaVenta para ventas recientes
            val timestamp = if (isVentaReciente) venta.fechaVenta else venta.fechaSeguimiento
            
            tvFecha.text = sdfFecha.format(Date(timestamp)).lowercase()
            tvHora.text = sdfHora.format(Date(timestamp))

            if (isVentaReciente) {
                ivIcon.setImageResource(R.drawable.ic_ventas)
                ivIcon.setColorFilter(android.graphics.Color.parseColor("#27AE60"))
                viewIconBg.setBackgroundResource(R.drawable.background_circle_light_green)
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_popup_reminder)
                ivIcon.setColorFilter(android.graphics.Color.parseColor("#F2994A"))
                viewIconBg.setBackgroundResource(R.drawable.background_circle_light_orange)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Venta>() {
        override fun areItemsTheSame(oldItem: Venta, newItem: Venta): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Venta, newItem: Venta): Boolean = oldItem == newItem
    }
}
