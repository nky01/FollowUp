package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.app.AlertDialog
// Diálogo estándar Android

import android.app.Dialog
// Diálogo personalizado

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
// Validación de email

import android.view.*
import android.widget.*

import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.followup.R
import com.followup.data.adapter.ClientesAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente

import com.followup.presentation.settings.SessionManager

import com.google.android.material.button.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.*

import kotlinx.coroutines.launch


/* ----------------------------------------------------------------------------------------
                                      FRAGMENT CLIENTES
---------------------------------------------------------------------------------------- */
/*
    [+] - Muestra la lista de clientes
    [+] - CRUD completo
    [+] - Filtros + búsqueda
    [+] - Manejo de UI (FAB + dialogs)
*/

class ClientesFragment : Fragment() {

    /* ----------------------------------------------------------------------------------------
                                            ATRIBUTOS
    ---------------------------------------------------------------------------------------- */

    // -------- UI --------
    private lateinit var rvClientes: RecyclerView
    private lateinit var searchInput: TextInputEditText

    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenuContainer: View
    private lateinit var fabOverlay: View
    private lateinit var btnNuevoCliente: MaterialButton

    // -------- Filtros --------
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var btnFiltroTodos: MaterialButton
    private lateinit var btnFiltroVendidos: MaterialButton
    private lateinit var btnFiltroPendiente: MaterialButton

    // -------- Data --------
    private lateinit var adapter: ClientesAdapter
    private val clientesCargados = mutableListOf<Cliente>()

    private var filtroActual: String? = null
    private var isFabMenuOpen = false

    private lateinit var sessionManager: SessionManager


    /* ----------------------------------------------------------------------------------------
                                      CICLO DE VIDA
    ---------------------------------------------------------------------------------------- */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initServices()
        initViews(view)
        initRecycler()
        initListeners()
        initDefaultState()

        cargarClientes()
    }

    override fun onResume() {
        super.onResume()
        cargarClientes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }


    /* ----------------------------------------------------------------------------------------
                                      INICIALIZACIÓN
    ---------------------------------------------------------------------------------------- */

    private fun initServices() {
        sessionManager = SessionManager(requireContext())
    }

    private fun initViews(view: View) {
        rvClientes = view.findViewById(R.id.rv_clientes)
        searchInput = view.findViewById(R.id.search)

        fabMain = view.findViewById(R.id.fab_main)
        fabMenuContainer = view.findViewById(R.id.fab_menu_container)
        fabOverlay = view.findViewById(R.id.fab_overlay)
        btnNuevoCliente = view.findViewById(R.id.btn_nuevo_cliente)

        btnFiltroTodos = view.findViewById(R.id.btn_filter_todos)
        btnFiltroVendidos = view.findViewById(R.id.btn_filter_vendidos)
        btnFiltroPendiente = view.findViewById(R.id.btn_filter_pendiente)

        filterGroup = view.findViewById(R.id.filter_group)
    }

    private fun initRecycler() {

        rvClientes.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClientesAdapter(object : ClientesAdapter.OnClienteClickListener {

            override fun onDeleteClick(cliente: Cliente) {
                mostrarDialogEliminar(cliente)
            }

            override fun onEditClick(cliente: Cliente) {
                mostrarDialogEditar(cliente)
            }
        })

        rvClientes.adapter = adapter
    }

    private fun initDefaultState() {
        filterGroup.check(R.id.btn_filter_todos)
        actualizarEstilosFiltros(R.id.btn_filter_todos)
    }


    /* ----------------------------------------------------------------------------------------
                                      LISTENERS
    ---------------------------------------------------------------------------------------- */

    private fun initListeners() {

        // FAB
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevoCliente.setOnClickListener {
            closeFabMenu()
            showNuevoClienteDialog()
        }

        // Búsqueda
        searchInput.doAfterTextChanged {
            aplicarBusquedaYRender(it.toString())
        }

        // Filtros
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            filtroActual = when (checkedId) {
                R.id.btn_filter_vendidos -> "Vendido"
                R.id.btn_filter_pendiente -> "Pendiente"
                else -> null
            }

            actualizarEstilosFiltros(checkedId)
            cargarClientes()
        }
    }


    /* ----------------------------------------------------------------------------------------
                                      FAB MENU
    ---------------------------------------------------------------------------------------- */

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {

        isFabMenuOpen = true

        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE

        fabOverlay.animate().alpha(1f).setDuration(150).start()
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(180).start()

        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun closeFabMenu() {

        if (!isFabMenuOpen) return

        isFabMenuOpen = false

        fabOverlay.animate().alpha(0f).withEndAction {
            fabOverlay.visibility = View.GONE
        }.start()

        fabMenuContainer.animate().alpha(0f).translationY(20f).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()

        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }


    /* ----------------------------------------------------------------------------------------
                                      FILTROS / BÚSQUEDA
    ---------------------------------------------------------------------------------------- */

    private fun actualizarEstilosFiltros(selectedId: Int) {

        val botones = listOf(btnFiltroTodos, btnFiltroVendidos, btnFiltroPendiente)

        botones.forEach {
            if (it.id == selectedId) {
                it.setBackgroundColor(Color.parseColor("#286DFF"))
                it.setTextColor(Color.WHITE)
            } else {
                it.setBackgroundColor(Color.WHITE)
                it.setTextColor(Color.parseColor("#475467"))
            }
        }
    }

    private fun aplicarBusquedaYRender(query: String) {

        val texto = query.trim().lowercase()

        val listaFiltrada = if (texto.isEmpty()) {
            clientesCargados
        } else {
            clientesCargados.filter { coincideBusqueda(it, texto) }
        }

        adapter.submitList(listaFiltrada)
    }

    // 🔹 Separamos la lógica de búsqueda (más limpio y reutilizable)
    private fun coincideBusqueda(cliente: Cliente, texto: String): Boolean {
        return cliente.nombre.lowercase().contains(texto) ||
                cliente.email.lowercase().contains(texto) ||
                cliente.telefono.lowercase().contains(texto) ||
                cliente.descripcion.lowercase().contains(texto)
    }


    /* ----------------------------------------------------------------------------------------
                                      BASE DE DATOS
    ---------------------------------------------------------------------------------------- */

    private fun cargarClientes() {

        lifecycleScope.launch {

            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()
            val userMail = sessionManager.getUserMail()

            val clientes = if (filtroActual == null) {
                dao.obtenerTodos(userMail)
            } else {
                dao.obtenerPorEstado(filtroActual!!, userMail)
            }

            clientesCargados.clear()
            clientesCargados.addAll(clientes)

            aplicarBusquedaYRender(searchInput.text.toString())
        }
    }


    /* ----------------------------------------------------------------------------------------
                                      CRUD - ELIMINAR
    ---------------------------------------------------------------------------------------- */

    private fun mostrarDialogEliminar(cliente: Cliente) {

        val dialog = Dialog(requireContext())

        val view = layoutInflater.inflate(R.layout.dialog_eliminar_cliente, null)
        dialog.setContentView(view)

        // Eliminar fondo por defecto
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Opcional (pero recomendado)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        view.findViewById<TextView>(R.id.tv_titulo_eliminar)
            .text = "¿Eliminar a ${cliente.nombre}?"

        view.findViewById<MaterialButton>(R.id.btn_eliminar_cliente).setOnClickListener {
            eliminarClienteConfirmado(cliente)
            dialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun eliminarClienteConfirmado(cliente: Cliente) {

        lifecycleScope.launch {

            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()

            dao.marcarComoEliminado(cliente.id)

            cargarClientes()

            Toast.makeText(requireContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show()
        }
    }


    /* ----------------------------------------------------------------------------------------
                                      VALIDACIONES
    ---------------------------------------------------------------------------------------- */

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

        var valido = true

        if (nombre.isEmpty()) {
            tilNombre.error = "Obligatorio"
            valido = false
        }

        if (telefono.length < 8) {
            tilTelefono.error = "Teléfono inválido"
            valido = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email inválido"
            valido = false
        }

        if (estado.isEmpty()) {
            tilEstado.error = "Seleccionar estado"
            valido = false
        }

        return valido
    }


    /* ----------------------------------------------------------------------------------------
                                      DIALOGOS
    ---------------------------------------------------------------------------------------- */

    private fun mostrarDialogEditar(cliente: Cliente) {

        val view = layoutInflater.inflate(R.layout.dialog_editar_cliente, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val views = obtenerViewsEditar(view)

        configurarDropdownEstado(views.estado)
        cargarDatosClienteEnDialog(views, cliente)
        setupValidacionesEnTiempoReal(views)

        views.btnGuardar.setOnClickListener {
            procesarEdicionCliente(cliente, views, dialog)
        }

        views.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun obtenerViewsEditar(view: View) = EditarClienteViews(
        nombre = view.findViewById(R.id.tiet_cliente_nombre),
        telefono = view.findViewById(R.id.tiet_cliente_telefono),
        email = view.findViewById(R.id.tiet_cliente_email),
        estado = view.findViewById(R.id.actv_cliente_estado),
        descripcion = view.findViewById(R.id.tiet_cliente_descripcion),
        tilNombre = view.findViewById(R.id.til_cliente_nombre),
        tilTelefono = view.findViewById(R.id.til_cliente_telefono),
        tilEmail = view.findViewById(R.id.til_cliente_email),
        tilEstado = view.findViewById(R.id.til_cliente_estado),
        btnGuardar = view.findViewById(R.id.btn_guardar_cliente),
        btnCancelar = view.findViewById(R.id.btn_cancelar_cliente)
    )

    private fun cargarDatosClienteEnDialog(v: EditarClienteViews, cliente: Cliente) {

        v.nombre.setText(cliente.nombre)
        v.telefono.setText(cliente.telefono)
        v.email.setText(cliente.email)
        v.descripcion.setText(cliente.descripcion)
        v.estado.setText(cliente.estado, false)
    }

    private fun setupValidacionesEnTiempoReal(v: EditarClienteViews) {
        v.nombre.doAfterTextChanged { v.tilNombre.error = null }
        v.telefono.doAfterTextChanged { v.tilTelefono.error = null }
        v.email.doAfterTextChanged { v.tilEmail.error = null }
        v.estado.doAfterTextChanged { v.tilEstado.error = null }
    }

    /* ---------- LÓGICA DE FORMULARIOS ---------- */
    private fun procesarEdicionCliente(
        clienteOriginal: Cliente,
        v: EditarClienteViews,
        dialog: AlertDialog
    ) {

        val nombre = v.nombre.text.toString().trim()
        val telefono = v.telefono.text.toString().trim()
        val email = v.email.text.toString().trim()
        val estado = v.estado.text.toString().trim()
        val descripcion = v.descripcion.text.toString().trim()

        val esValido = validarClienteFrontend(
            nombre,
            telefono,
            email,
            estado,
            v.tilNombre,
            v.tilTelefono,
            v.tilEmail,
            v.tilEstado
        )

        if (!esValido) return

        val clienteActualizado = clienteOriginal.copy(
            nombre = nombre,
            telefono = telefono,
            email = email,
            descripcion = descripcion,
            estado = estado
        )

        actualizarCliente(clienteActualizado, clienteOriginal.email, v.tilEmail, dialog)
    }

    private fun showNuevoClienteDialog() {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_nuevo_cliente)

        val views = obtenerViewsNuevoCliente(dialog)

        configurarDropdownEstado(views.estado)

        views.btnGuardar.setOnClickListener {
            procesarNuevoCliente(dialog, views)
        }

        views.btnCancelar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // 🔹 Encapsulamos referencias (clave para no ensuciar)
    private fun obtenerViewsNuevoCliente(dialog: Dialog) = NuevoClienteViews(
        nombre = dialog.findViewById(R.id.tiet_cliente_nombre),
        telefono = dialog.findViewById(R.id.tiet_cliente_telefono),
        email = dialog.findViewById(R.id.tiet_cliente_email),
        estado = dialog.findViewById(R.id.actv_cliente_estado),
        descripcion = dialog.findViewById(R.id.tiet_cliente_descripcion),
        tilNombre = dialog.findViewById(R.id.til_cliente_nombre),
        tilTelefono = dialog.findViewById(R.id.til_cliente_telefono),
        tilEmail = dialog.findViewById(R.id.til_cliente_email),
        tilEstado = dialog.findViewById(R.id.til_cliente_estado),
        btnGuardar = dialog.findViewById(R.id.btn_guardar_cliente),
        btnCancelar = dialog.findViewById(R.id.btn_cancelar_cliente)
    )

    private object EstadoCliente {
        const val NUEVO = "Nuevo cliente"
        const val POTENCIAL = "Cliente potencial"
        const val LLAMAR = "Llamar"
        const val PENDIENTE = "Pendiente"
    }
    private fun configurarDropdownEstado(view: AutoCompleteTextView) {
        val estados = listOf(
            EstadoCliente.NUEVO,
            EstadoCliente.POTENCIAL,
            EstadoCliente.LLAMAR,
            EstadoCliente.PENDIENTE
        )

        view.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))
    }

    private fun procesarNuevoCliente(dialog: Dialog, v: NuevoClienteViews) {

        val nombre = v.nombre.text.toString().trim()
        val telefono = v.telefono.text.toString().trim()
        val email = v.email.text.toString().trim()
        val estado = v.estado.text.toString().trim()
        val descripcion = v.descripcion.text.toString().trim()

        if (validarClienteFrontend(nombre, telefono, email, estado, v.tilNombre, v.tilTelefono, v.tilEmail, v.tilEstado)) {
            registrarCliente(nombre, telefono, email, estado, descripcion, dialog, v.tilEmail)
        }
    }


    /* ----------------------------------------------------------------------------------------
                                      MODELO AUXILIAR (UI)
    ---------------------------------------------------------------------------------------- */

    // 🔹 Esto es CLAVE: reduce ruido visual brutalmente
    data class NuevoClienteViews(
        val nombre: TextInputEditText,
        val telefono: TextInputEditText,
        val email: TextInputEditText,
        val estado: AutoCompleteTextView,
        val descripcion: TextInputEditText,
        val tilNombre: TextInputLayout,
        val tilTelefono: TextInputLayout,
        val tilEmail: TextInputLayout,
        val tilEstado: TextInputLayout,
        val btnGuardar: MaterialButton,
        val btnCancelar: MaterialButton
    )

    data class EditarClienteViews(
        val nombre: TextInputEditText,
        val telefono: TextInputEditText,
        val email: TextInputEditText,
        val estado: AutoCompleteTextView,
        val descripcion: TextInputEditText,
        val tilNombre: TextInputLayout,
        val tilTelefono: TextInputLayout,
        val tilEmail: TextInputLayout,
        val tilEstado: TextInputLayout,
        val btnGuardar: MaterialButton,
        val btnCancelar: MaterialButton
    )


    /* ----------------------------------------------------------------------------------------
                                      CRUD - DB
    ---------------------------------------------------------------------------------------- */

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

            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()
            val userMail = sessionManager.getUserMail()

            val existe = dao.obtenerPorEmail(email, userMail)

            if (existe == null) {

                dao.insert(
                    Cliente(
                        nombre = nombre,
                        telefono = telefono,
                        email = email,
                        descripcion = descripcion,
                        estado = estado,
                        fecha = System.currentTimeMillis(),
                        userMail = userMail
                    )
                )

                cargarClientes()
                Toast.makeText(requireContext(), "Cliente guardado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

            } else {
                tilEmail.error = "Email ya en uso"
            }
        }
    }

    private fun actualizarCliente(
        cliente: Cliente,
        emailOriginal: String,
        tilEmail: TextInputLayout,
        dialog: AlertDialog
    ) {
        lifecycleScope.launch {

            val dao = AppDatabase.getDatabase(requireContext()).clienteDao()

            if (cliente.email != emailOriginal) {

                val userMail = sessionManager.getUserMail()
                val existe = dao.obtenerPorEmail(cliente.email, userMail)

                if (existe != null) {
                    tilEmail.error = "Email ya en uso"
                    return@launch
                }
            }

            dao.update(cliente)

            cargarClientes()
            Toast.makeText(requireContext(), "Cliente actualizado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

}