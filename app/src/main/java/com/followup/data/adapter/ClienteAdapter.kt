package com.followup.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.entity.Cliente

class ClienteAdapter(
    private val listener: OnClienteClickListener
) : RecyclerView.Adapter<ClienteAdapter.ViewHolder>() {

    // 🔥 LISTAS IMPORTANTES
    private var listaOriginal: List<Cliente> = emptyList()
    private var listaMostrada: List<Cliente> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre = view.findViewById<TextView>(R.id.tvNombre)
        val descripcion = view.findViewById<TextView>(R.id.tvDescripcion)
        val telefono = view.findViewById<TextView>(R.id.tvTelefono)
        val email = view.findViewById<TextView>(R.id.tvEmail)
        val estado = view.findViewById<TextView>(R.id.tvEstado)

        val expandible = view.findViewById<LinearLayout>(R.id.layoutExpandible)
        val arrow = view.findViewById<ImageView>(R.id.imgArrow)

        val delete = view.findViewById<ImageView>(R.id.btnEliminar)
        val edit = view.findViewById<ImageView>(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cliente = listaMostrada[position] // 🔥 IMPORTANTE

        holder.nombre.text = cliente.nombre
        holder.descripcion.text = cliente.descripcion
        holder.telefono.text = cliente.telefono
        holder.email.text = cliente.email
        holder.estado.text = cliente.estado

        holder.expandible.visibility =
            if (cliente.expandido) View.VISIBLE else View.GONE

        holder.arrow.rotation =
            if (cliente.expandido) 180f else 0f

        holder.arrow.setOnClickListener {
            cliente.expandido = !cliente.expandido
            notifyItemChanged(position)
        }

        holder.delete.setOnClickListener {
            listener.onDeleteClick(cliente)
        }

        holder.edit.setOnClickListener {
            listener.onEditClick(cliente)
        }
    }

    override fun getItemCount(): Int = listaMostrada.size // 🔥 IMPORTANTE

    // 🔹 CUANDO VIENE DE LA DB
    fun actualizarLista(nuevaLista: List<Cliente>) {
        listaOriginal = nuevaLista
        listaMostrada = nuevaLista
        notifyDataSetChanged()
    }

    // 🔹 FILTRO
    fun filtrar(estado: String) {
        listaMostrada = when (estado) {
            "TODOS" -> listaOriginal
            else -> listaOriginal.filter {
                it.estado.equals(estado, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    interface OnClienteClickListener {
        fun onDeleteClick(cliente: Cliente)
        fun onEditClick(cliente: Cliente)
    }
}