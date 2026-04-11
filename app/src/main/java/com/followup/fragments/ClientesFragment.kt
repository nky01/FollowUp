package com.followup.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ClientesFragment : Fragment() {
// lateinit es una promesa de que la variable se inicializará antes de su uso.
    private lateinit var fabMain: FloatingActionButton // Referencia al botón flotante principal
    private lateinit var fabMenuContainer: View // Contenedor del menú del botón flotante
    private lateinit var fabOverlay: View // Vista de superposición para oscurecer el fondo cuando el menú está abierto
    private var isFabMenuOpen = false // Bandera para controlar el estado del menú del botón flotante
    private lateinit var rvClientes: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private val clientesCargados = mutableListOf<Cliente>()
    private var filtroActual: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    //
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fabMain = view.findViewById(R.id.fab_main)
        fabMenuContainer = view.findViewById(R.id.fab_menu_container)
        fabOverlay = view.findViewById(R.id.fab_overlay)
        val btnNuevoCliente = view.findViewById<MaterialButton>(R.id.btn_nuevo_cliente)

        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false

        rvClientes = view.findViewById(R.id.rv_clientes)
        searchInput = view.findViewById(R.id.search)
        val btnFiltroTodos = view.findViewById<MaterialButton>(R.id.btn_filter_todos)
        val btnFiltroVendidos = view.findViewById<MaterialButton>(R.id.btn_filter_vendidos)
        val btnFiltroPendiente = view.findViewById<MaterialButton>(R.id.btn_filter_pendiente)

        setupClientesList()
        cargarClientes()

        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevoCliente.setOnClickListener {
            closeFabMenu()
            showNuevoClienteDialog()
        }

        searchInput.doAfterTextChanged { texto ->
            if (isFabMenuOpen) closeFabMenu()
            aplicarBusquedaYRender(texto?.toString().orEmpty())
        }

        btnFiltroTodos.setOnClickListener {
            if (isFabMenuOpen) closeFabMenu()
            filtroActual = null
            cargarClientes()
        }

        btnFiltroVendidos.setOnClickListener {
            if (isFabMenuOpen) closeFabMenu()
            filtroActual = "Vendido"
            cargarClientes()
        }

        btnFiltroPendiente.setOnClickListener {
            if (isFabMenuOpen) closeFabMenu()
            filtroActual = "Pendiente"
            cargarClientes()
        }
    }

    private fun setupClientesList() {
        rvClientes.layoutManager = LinearLayoutManager(requireContext())
        rvClientes.adapter = ClientesAdapter()
    }

    // cargarClientes se encarga de obtener la lista de clientes desde la base de datos, aplicando el filtro de estado si es necesario, y dsp actualiza la vista
    private fun cargarClientes() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()
            val clientes = if (filtroActual == null) {
                dao.obtenerTodos()
            } else {
                dao.obtenerPorEstado(filtroActual!!)
            }

            clientesCargados.clear()
            clientesCargados.addAll(clientes)
            aplicarBusquedaYRender(searchInput.text?.toString().orEmpty())
        }
    }

    private fun aplicarBusquedaYRender(query: String) {
        val texto = query.trim().lowercase()
        if (texto.isEmpty()) {
            (rvClientes.adapter as ClientesAdapter).submitList(clientesCargados)
            return
        }

        val filtrados = clientesCargados.filter { cliente ->
            cliente.nombre.lowercase().contains(texto) ||
                cliente.email.lowercase().contains(texto) ||
                cliente.telefono.lowercase().contains(texto) ||
                cliente.descripcion.lowercase().contains(texto)
        }
        (rvClientes.adapter as ClientesAdapter).submitList(filtrados)
    }

    // toggleFabMenu se encarga de alternar entre abrir y cerrar el btn flotante
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    // openFabMenu se encarga de mostrar el menu del btn flotante con animaciones,
    private fun openFabMenu() {
        isFabMenuOpen = true
        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE
        fabOverlay.isClickable = true
        fabOverlay.isFocusable = true

        // Fuerza el orden visual correcto en tiempo de ejecución.
        fabOverlay.bringToFront()
        fabMenuContainer.bringToFront()
        fabMain.bringToFront()

        fabOverlay.alpha = 0f
        fabOverlay.animate().alpha(1f).setDuration(150).start()

        fabMenuContainer.alpha = 0f
        fabMenuContainer.translationY = 20f
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(180).start()

        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    // closeFabMenu se encarga de ocultar el menu del btn flotante con animaciones
    private fun closeFabMenu() {
        if (!isFabMenuOpen) return

        isFabMenuOpen = false
        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false
        fabOverlay.animate().alpha(0f).setDuration(120).withEndAction {
            fabOverlay.visibility = View.GONE
        }.start()

        fabMenuContainer.animate().alpha(0f).translationY(20f).setDuration(140).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()

        fabMain.bringToFront()
        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }

    // showNuevoClienteDialog se encarga de mostrar un dialogo personalizado para registrar un nuevo cliente, con validaciones y guardado en la base de datos
    private fun showNuevoClienteDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_nuevo_cliente)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tilNombre = dialog.findViewById<TextInputLayout>(R.id.til_cliente_nombre)
        val tietNombre = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_nombre)
        val tilTelefono = dialog.findViewById<TextInputLayout>(R.id.til_cliente_telefono)
        val tietTelefono = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_telefono)
        val tilEmail = dialog.findViewById<TextInputLayout>(R.id.til_cliente_email)
        val tietEmail = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_email)
        val tilEstado = dialog.findViewById<TextInputLayout>(R.id.til_cliente_estado)
        val actvEstado = dialog.findViewById<AutoCompleteTextView>(R.id.actv_cliente_estado)
        val tietDescripcion = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_descripcion)
        val btnCancelar = dialog.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)
        val btnGuardar = dialog.findViewById<MaterialButton>(R.id.btn_guardar_cliente)

        val estados = listOf("Pendiente", "Vendido", "No Asignado")
        actvEstado.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnGuardar.setOnClickListener {
            val nombre = tietNombre.text.toString().trim()
            val telefono = tietTelefono.text.toString().trim()
            val email = tietEmail.text.toString().trim()
            val estado = actvEstado.text.toString().trim()
            val descripcion = tietDescripcion.text.toString().trim()

            if (validarClienteFrontend(nombre, telefono, email, estado, tilNombre, tilTelefono, tilEmail, tilEstado)) {
                registrarCliente(nombre, telefono, email, estado, descripcion, dialog, tilEmail)
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // validarClienteFrontend se encarga de validar los campos del formulario de nuevo cliente, mostrando errores en los TextInputLayout correspondientes
    private fun validarClienteFrontend(
        nombre: String,
        telefono: String,
        email: String,
        estado: String,
        tilNombre: TextInputLayout,
        tilTelefono: TextInputLayout,
        tilEmail: TextInputLayout,
        tilEstado: TextInputLayout
    ): Boolean {
        var esValido = true

        if (nombre.isEmpty()) {
            tilNombre.error = "El nombre es obligatorio"
            esValido = false
        } else {
            tilNombre.error = null
        }

        if (telefono.isEmpty()) {
            tilTelefono.error = "El telefono es obligatorio"
            esValido = false
        } else if (telefono.length < 8) {
            tilTelefono.error = "Ingrese un telefono valido"
            esValido = false
        } else {
            tilTelefono.error = null
        }

        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email invalido"
            esValido = false
        } else {
            tilEmail.error = null
        }

        if (estado.isEmpty()) {
            tilEstado.error = "Seleccione un estado"
            esValido = false
        } else {
            tilEstado.error = null
        }

        return esValido
    }

    // registrarCliente se encarga de guardar el nuevo cliente en la base de datos, verificando que el email no exista previamente, y mostrando mensajes de exito o error
    private fun registrarCliente(
        nombre: String,
        telefono: String,
        email: String,
        estado: String,
        descripcion: String,
        dialog: Dialog,
        tilEmail: TextInputLayout
    ) {
        lifecycleScope.launch {
            try {
                // Verificar si el email ya existe en la base de datos
                val database = AppDatabase.getDatabase(requireContext())
                val dao = database.clienteDao()
                val existe = dao.obtenerPorEmail(email)

                if (existe == null) {
                    dao.insert(
                        Cliente(
                            nombre = nombre,
                            descripcion = descripcion,
                            telefono = telefono,
                            email = email,
                            estado = estado,
                            fecha = System.currentTimeMillis()
                        )
                    )
                    cargarClientes()
                    Toast.makeText(requireContext(), "Cliente guardado con exito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    tilEmail.error = "Este correo ya esta en uso"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al guardar cliente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ClientesAdapter es el adaptador del RecyclerView que muestra la lista de clientes,
    // yo lo desarrolle de esta forma para mantenerlo dentro del mismo archivo,
    // pero se podria extraer a un archivo separado si se quisiera. Es lo mas recomendable.
    private class ClientesAdapter : RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder>() {
        private val items = mutableListOf<Cliente>()
        private val dateFormatter = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-AR"))

        fun submitList(clientes: List<Cliente>) {
            items.clear()
            items.addAll(clientes)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
            return ClienteViewHolder(view)
        }

        override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
            holder.bind(items[position], dateFormatter)
        }

        override fun getItemCount(): Int = items.size

        // ClienteViewHolder es la clase que representa cada item de cliente en el RecyclerView.
        class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
            private val tvSubtitulo: TextView = itemView.findViewById(R.id.tvSubtitulo)
            private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
            private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
            private val tvDescTag: TextView = itemView.findViewById(R.id.tvDescTag)
            private val tvTelefonoValue: TextView = itemView.findViewById(R.id.tvTelefonoValue)
            private val tvEmailValue: TextView = itemView.findViewById(R.id.tvEmailValue)
            private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
            private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

            // bind se encarga de asignar los datos del cliente a las vistas del item, y de configurar los colores y acciones segun el estado del cliente
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

                // Por ahora solo se muestra visualmente; las acciones se implementaran luego.
                ivEdit.setOnClickListener(null)
                ivDelete.setOnClickListener(null)
            }

            // estadoColors devuelve un par de colores (fondo y texto) segun el estado del cliente.
            private fun estadoColors(estado: String): Pair<Int, Int> {
                return when (estado.lowercase(Locale.ROOT)) {
                    "vendido" -> Pair(Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"))
                    "pendiente" -> Pair(Color.parseColor("#FFF4E5"), Color.parseColor("#D4850D"))
                    else -> Pair(Color.parseColor("#F2F4F7"), Color.parseColor("#475467"))
                }
            }
        }
    }

    // onDestreoyView lo recomienda usar, por un tema de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }
}