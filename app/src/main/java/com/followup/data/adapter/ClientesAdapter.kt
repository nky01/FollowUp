package com.followup.data.adapter

import com.followup.data.entity.Cliente
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import java.text.SimpleDateFormat
import java.util.*

class ClientesAdapter(
    private val listener: OnClienteClickListener // LISTENER PARA EL CLICK EN EL ITEM DEL RECYCLERVIEW
) : RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder>() {

    private val items = mutableListOf<Cliente>()
    private val dateFormatter = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-AR"))

    /* ---------------------------------------------------------------------------
         INTERFAZ PARA EL CLICK EN EL ITEM DEL RECYCLERVIEW ( EDIT / DELETE )
    --------------------------------------------------------------------------- */
    interface OnClienteClickListener {
        fun onDeleteClick(cliente: Cliente)
        fun onEditClick(cliente: Cliente)
    }

    fun submitList(clientes: List<Cliente>) {
        items.clear()
        items.addAll(clientes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        holder.bind(items[position], dateFormatter)
    }

    override fun getItemCount(): Int = items.size

    class ClienteViewHolder(
        itemView: View,
        private val listener: OnClienteClickListener
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

        fun bind(cliente: Cliente, formatter: SimpleDateFormat) {
            tvNombre.text = cliente.nombre
            tvSubtitulo.text = ""
            tvDescTag.text = if (cliente.descripcion.isBlank()) "Sin descripcion" else cliente.descripcion
            tvEstado.text = cliente.estado
            tvFecha.text = formatter.format(Date(cliente.fecha))
            tvTelefonoValue.text = cliente.telefono
            tvEmailValue.text = cliente.email

            val colors = estadoColors(cliente.estado)
            tvEstado.setBackgroundColor(colors.first)
            tvEstado.setTextColor(colors.second)

            // EDITAR
            ivEdit.setOnClickListener {
                listener.onEditClick(cliente)
            }

            // ELIMINAR
            ivDelete.setOnClickListener {
                listener.onDeleteClick(cliente)
            }

            // ESTADO INICIAL DE LOS COMPONENTES ( IMPORTANTE PARA EL RECYCLERVIEW )
            layoutContacto.visibility = if (cliente.expandido) View.VISIBLE else View.GONE
            ivToggle.rotation = if (cliente.expandido) 90f else 0f

            ivToggle.setOnClickListener {

                cliente.expandido = !cliente.expandido // CAMBIA ESTADO

                if (cliente.expandido) {
                    // MOSTRAR
                    layoutContacto.visibility = View.VISIBLE

                    ivToggle.animate()
                        .rotation(90f)
                        .setDuration(200)
                        .start()

                } else {
                    // OCULTAR
                    layoutContacto.visibility = View.GONE

                    ivToggle.animate()
                        .rotation(0f)
                        .setDuration(200)
                        .start()
                }
            }
        }
        private fun estadoColors(estado: String): Pair<Int, Int> {
            return when (estado.lowercase(Locale.ROOT)) {

                "venta finalizada" -> Pair(
                    Color.parseColor("#E8F5E9"),
                    Color.parseColor("#2E7D32")
                )

                "pendiente" -> Pair(
                    Color.parseColor("#FFF4E5"),
                    Color.parseColor("#D4850D")
                )

                "nuevo cliente" -> Pair(
                    Color.parseColor("#E3F2FD"),
                    Color.parseColor("#1E88E5")
                )

                "cliente potencial" -> Pair(
                    Color.parseColor("#F3E5F5"),
                    Color.parseColor("#8E24AA")
                )

                "llamar" -> Pair(
                    Color.parseColor("#FFFDE7"),
                    Color.parseColor("#F9A825")
                )

                "sin asignar" -> Pair(
                    Color.parseColor("#F2F4F7"),
                    Color.parseColor("#98A2B3")
                )

                else -> Pair(
                    Color.parseColor("#F2F4F7"),
                    Color.parseColor("#475467")
                )
            }
        }
    }
}