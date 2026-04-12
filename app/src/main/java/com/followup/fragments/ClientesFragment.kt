package com.followup.fragments

import android.app.AlertDialog
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
import com.followup.data.adapter.ClientesAdapter
import com.google.android.material.button.MaterialButtonToggleGroup

class ClientesFragment : Fragment() {

    /* ------------------------------------------------------------------------------------
                                            ATRIBUTOS
    ------------------------------------------------------------------------------------ */
    // lateinit -> Indica que la variable sera inicializada mas adelante, pero no inmediatamente.

    private lateinit var fabMain: FloatingActionButton // Referencia al botón flotante principal
    private lateinit var fabMenuContainer: View // Contenedor del menú del botón flotante
    private lateinit var fabOverlay: View // Vista de superposición para oscurecer el fondo cuando el menú está abierto
    private var isFabMenuOpen =
        false // Bandera para controlar el estado del menú del botón flotante
    private lateinit var rvClientes: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private val clientesCargados = mutableListOf<Cliente>()
    private var filtroActual: String? = null
    private lateinit var btnNuevoCliente: MaterialButton // Botón para agregar un nuevo cliente

    private lateinit var btnFiltroTodos: MaterialButton // Botón Filtrar Todos
    private lateinit var btnFiltroVendidos: MaterialButton // Botón Filtrar Todos
    private lateinit var btnFiltroPendiente: MaterialButton // Botón Filtrar Todos

    private lateinit var filterGroup: MaterialButtonToggleGroup // GRUPO DE BOTONES PARA FILTRAR CLIENTES

    private lateinit var adapter: ClientesAdapter

    /* ------------------------------------------------------------------------------------
                                      MÉTODOS DEL FRAGMENT
    ------------------------------------------------------------------------------------ */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    // INICIALIZADOR DE LOS COMPONENTES Y LISTENERS
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        initComponents(view) // INICIALIZA LOS COMPONENTES DE LA VISTA

        setupClientesList()
        cargarClientes()

        initListeners() // INICIALIZA LOS LISTENERS DE LA VISTA

        // ESTADO INICIAL DE LOS FILTROS
        filterGroup.check(R.id.btn_filter_todos) // SELECCIONA "TODOS"
        actualizarEstilosFiltros(R.id.btn_filter_todos) // APLICA COLORES

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

    }

    /* ------------------------------------------------------------------------------------
                                      MÉTODOS PROPIOS
    ------------------------------------------------------------------------------------ */

    /* --------------------------------------------------
          INICIALIZADOR DE COMPONENTES DE LA VISTA
    -------------------------------------------------- */
    private fun initComponents(view: View) { // Recibe la vista del fragmento

        fabMain = view.findViewById(R.id.fab_main)
        fabMenuContainer = view.findViewById(R.id.fab_menu_container)
        fabOverlay = view.findViewById(R.id.fab_overlay)
        btnNuevoCliente = view.findViewById(R.id.btn_nuevo_cliente)

        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false

        rvClientes = view.findViewById(R.id.rv_clientes)
        searchInput = view.findViewById(R.id.search)
        btnFiltroTodos = view.findViewById<MaterialButton>(R.id.btn_filter_todos)
        btnFiltroVendidos = view.findViewById<MaterialButton>(R.id.btn_filter_vendidos)
        btnFiltroPendiente = view.findViewById<MaterialButton>(R.id.btn_filter_pendiente)

        filterGroup =
            view.findViewById(R.id.filter_group) // INICIALIZA EL GRUPO DE BOTONES PARA FILTRAR CLIENTES
    }

    /* --------------------------------------------------
          INICIALIZADOR DE LISTENERS DE LA VISTA
    -------------------------------------------------- */
    private fun initListeners() {

        // SELECCIÓN DE FILTROS
        filterGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            // CAMBIA EL FILTRO SEGÚN EL BOTÓN SELECCIONADO
            when (checkedId) {
                R.id.btn_filter_todos -> filtroActual = null
                R.id.btn_filter_vendidos -> filtroActual = "Vendido"
                R.id.btn_filter_pendiente -> filtroActual = "Pendiente"
            }

            actualizarEstilosFiltros(checkedId) // CAMBIA LOS COLORES --> [ UI ]
            cargarClientes() // CARGA LOS CLIENTES SEGÚN EL FILTRO --> [ DATOS ]
        }

    }

    /* --------------------------------------------------
            ACTUALIZAR ESTILOS DE LOS FILTROS
    -------------------------------------------------- */
    private fun actualizarEstilosFiltros(selectedId: Int) {

        // AGRUPA LOS BOTONES EN UNA LISTA
        val botones = listOf(btnFiltroTodos, btnFiltroVendidos, btnFiltroPendiente)

        // RECORRE LA LISTA DE BOTONES
        for (boton in botones) {

            // PREGUNTA SI EL BOTÓN ACTUAL ES EL SELECCIONADO
            if (boton.id == selectedId) {
                // BOTÓN SELECCIONADO
                boton.setBackgroundColor(Color.parseColor("#286DFF")) // FONDO AZUL
                boton.setTextColor(Color.WHITE) // TEXTO BLANCO
            } else {
                // BOTÓN NO SELECCIONADO
                boton.setBackgroundColor(Color.WHITE) // FONDO BLANCO
                boton.setTextColor(Color.parseColor("#475467")) // TEXTO GRIS OSCURO
            }
        }

    }

    private fun setupClientesList() {
        rvClientes.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClientesAdapter(object : ClientesAdapter.OnClienteClickListener {

            override fun onDeleteClick(cliente: Cliente) {
                eliminarCliente(cliente)
            }

            override fun onEditClick(cliente: Cliente) {
                mostrarDialogEditar(cliente)
            }
        })

        rvClientes.adapter = adapter
    }

    private fun mostrarDialogEditar(cliente: Cliente) {

        /* --------- CREACIÓN DEL DIALOG */

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_cliente, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        /* --------- CREACIÓN DEL DIALOG END */
        /* --------- REFERENCIAR CAMPOS */

        val nombre = view.findViewById<TextInputEditText>(R.id.tiet_cliente_nombre)
        val telefono = view.findViewById<TextInputEditText>(R.id.tiet_cliente_telefono)
        val email = view.findViewById<TextInputEditText>(R.id.tiet_cliente_email)
        val descripcion = view.findViewById<TextInputEditText>(R.id.tiet_cliente_descripcion)
        val estado = view.findViewById<AutoCompleteTextView>(R.id.actv_cliente_estado)

        val btnGuardar = view.findViewById<MaterialButton>(R.id.btn_guardar_cliente)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)

        /* --------- REFERENCIAR CAMPOS END */
        /* --------- CARGAR DATOS ACTUALES */

        nombre.setText(cliente.nombre)
        telefono.setText(cliente.telefono)
        email.setText(cliente.email)
        descripcion.setText(cliente.descripcion)
        estado.setText(cliente.estado, false)

        /* --------- CARGAR DATOS ACTUALES END */
        /* --------- BOTÓN CANCELAR */

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        /* --------- BOTÓN CANCELAR END */
        /* --------- BOTÓN GUARDAR */

        btnGuardar.setOnClickListener {

            val clienteActualizado = cliente.copy(
                nombre = nombre.text.toString(),
                telefono = telefono.text.toString(),
                email = email.text.toString(),
                descripcion = descripcion.text.toString(),
                estado = estado.text.toString()
            )

            actualizarCliente(clienteActualizado)
            dialog.dismiss()
        }

        /* --------- BOTÓN GUARDAR END */
        /* --------- MOSTRAR EL DIALOG */

        dialog.show()

    }

    /* --------------------------------------------------
              ACTUALIZAR CLIENTE BASE DE DATOS
    -------------------------------------------------- */
    private fun actualizarCliente(cliente: Cliente) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()
            dao.update(cliente)

            cargarClientes()

            Toast.makeText(requireContext(), "Cliente actualizado", Toast.LENGTH_SHORT).show()
        }
    }

    /* --------------------------------------------------
                  ELIMINAR CLIENTE FRONTED
    -------------------------------------------------- */
    private fun eliminarCliente(cliente: Cliente) {

        val view =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_eliminar_cliente, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(view)

        // centraliza el dialog
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            attributes.gravity = android.view.Gravity.CENTER
        }

        val btnCancelar = view.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)
        val btnEliminar = view.findViewById<MaterialButton>(R.id.btn_eliminar_cliente)
        val tvTitulo = view.findViewById<TextView>(R.id.tv_titulo_eliminar)

        tvTitulo.text = "¿Eliminar a ${cliente.nombre}?"

        btnCancelar.setOnClickListener {
            btnCancelar.setBackgroundColor(Color.parseColor("#286DFF"))
            btnCancelar.setTextColor(Color.WHITE)

            view.postDelayed({ dialog.dismiss() }, 100)
        }

        btnEliminar.setOnClickListener {
            eliminarClienteConfirmado(cliente)
            dialog.dismiss()
        }

        // Verifica que el Fragment este vinculado y solo muestra el diálogo si la pantalla aun existe y no se está cerrando
        if (isAdded && !requireActivity().isFinishing) {
            dialog.show()
        }
    }

    /* --------------------------------------------------
               ELIMINAR CLIENTE BASE DE DATOS
    -------------------------------------------------- */
    private fun eliminarClienteConfirmado(cliente: Cliente) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()
            dao.delete(cliente)

            cargarClientes()

            Toast.makeText(requireContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show()
        }
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
        actvEstado.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                estados
            )
        )

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnGuardar.setOnClickListener {
            val nombre = tietNombre.text.toString().trim()
            val telefono = tietTelefono.text.toString().trim()
            val email = tietEmail.text.toString().trim()
            val estado = actvEstado.text.toString().trim()
            val descripcion = tietDescripcion.text.toString().trim()

            if (validarClienteFrontend(
                    nombre,
                    telefono,
                    email,
                    estado,
                    tilNombre,
                    tilTelefono,
                    tilEmail,
                    tilEstado
                )
            ) {
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
                    Toast.makeText(
                        requireContext(),
                        "Cliente guardado con exito",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                } else {
                    tilEmail.error = "Este correo ya esta en uso"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al guardar cliente", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // onDestreoyView lo recomienda usar, por un tema de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }

}