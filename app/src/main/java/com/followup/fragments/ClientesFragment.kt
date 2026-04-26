package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.ClientesAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.EstadoCliente
import com.followup.presentation.settings.SessionManager
import com.followup.ui.EstadoColorHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Calendar

class ClientesFragment : Fragment() {

    /* ========================================================================================
                                        COMPONENTES DE VISTA
       ======================================================================================== */

    private lateinit var rvClientes: RecyclerView
    private lateinit var searchInput: TextInputEditText

    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenuContainer: View
    private lateinit var fabOverlay: View
    private lateinit var btnNuevoCliente: MaterialButton

    private lateinit var btnFiltroEstado: MaterialCardView

    private lateinit var dropdownEstados: LinearLayout

    /* ========================================================================================
                                        ESTADO INTERNO
       ======================================================================================== */

    private lateinit var adapter: ClientesAdapter

    /** Lista base sin filtrar. Cada item guarda el cliente + sus contadores de ventas. */
    private val clientesCargados = mutableListOf<Triple<Cliente, Int, Int>>()

    private var filtroActual: String? = null
    private var isFabMenuOpen = false
    private lateinit var sessionManager: SessionManager

    /* ========================================================================================
                                        CICLO DE VIDA
       ======================================================================================== */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_clientes, container, false)
        
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        initViews(view)
        initRecycler()
        initListeners()
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

    /* ========================================================================================
                                        INICIALIZACIÓN
       ======================================================================================== */

    private fun initViews(view: View) {
        rvClientes         = view.findViewById(R.id.rv_clientes)
        searchInput        = view.findViewById(R.id.search)
        fabMain            = view.findViewById(R.id.fab_main)
        fabMenuContainer   = view.findViewById(R.id.fab_menu_container)
        fabOverlay         = view.findViewById(R.id.fab_overlay)
        btnNuevoCliente    = view.findViewById(R.id.btn_nuevo_cliente)
        btnFiltroEstado  = view.findViewById(R.id.btn_filtro_estado)
        dropdownEstados  = view.findViewById(R.id.dropdown_estados)
    }

    /** 2 columnas en el RecyclerView con clipChildren=false para que los avatares no se corten */
    private fun initRecycler() {
        rvClientes.layoutManager = GridLayoutManager(requireContext(), 2)
        rvClientes.clipChildren = false
        rvClientes.clipToPadding = false

        adapter = ClientesAdapter(object : ClientesAdapter.OnClienteClickListener {
            override fun onDeleteClick(cliente: Cliente) = mostrarDialogEliminar(cliente)
            override fun onEditClick(cliente: Cliente)   = mostrarDialogEditar(cliente)
            override fun onDetalleClick(cliente: Cliente) = mostrarDialogDetalle(cliente)
        })

        rvClientes.adapter = adapter
    }

    /* ========================================================================================
                                        LISTENERS
       ======================================================================================== */

    private fun initListeners() {
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevoCliente.setOnClickListener {
            closeFabMenu()
            showNuevoClienteDialog()
        }

        searchInput.doAfterTextChanged { aplicarBusquedaYRender(it.toString()) }

        // Botón que abre/cierra el dropdown
        btnFiltroEstado.setOnClickListener {
            dropdownEstados.visibility =
                if (dropdownEstados.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Opciones del dropdown
        requireView().findViewById<TextView>(R.id.filtro_todos).setOnClickListener {
            filtroActual = null
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
        requireView().findViewById<TextView>(R.id.filtro_nuevo_cliente).setOnClickListener {
            filtroActual = EstadoCliente.NUEVO_CLIENTE
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
        requireView().findViewById<TextView>(R.id.filtro_pago_pendiente).setOnClickListener {
            filtroActual = EstadoCliente.PAGO_PENDIENTE
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
        requireView().findViewById<TextView>(R.id.filtro_pago_realizado).setOnClickListener {
            filtroActual = EstadoCliente.PAGO_REALIZADO
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
        requireView().findViewById<TextView>(R.id.filtro_no_asignado).setOnClickListener {
            filtroActual = EstadoCliente.NO_ASIGNADO
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
        requireView().findViewById<TextView>(R.id.filtro_pago_caducado).setOnClickListener {
            filtroActual = EstadoCliente.PAGO_CADUCADO
            dropdownEstados.visibility = View.GONE
            cargarClientes()
        }
    }

    /* ========================================================================================
                                    CARGA Y FILTRADO DE CLIENTES
       ======================================================================================== */

    /**
     * Trae los clientes de la BD, recalcula estados vencidos (NUEVO_CLIENTE / PAGO_REALIZADO
     * que ya cumplieron sus 24hs) y luego aplica filtro + búsqueda.
     */
    private fun cargarClientes() {
        lifecycleScope.launch {
            val dao      = AppDatabase.getDatabase(requireContext()).clienteDao()
            val userMail = sessionManager.getUserMail()

            // 1. Recalcular estados vencidos antes de mostrar
            recalcularEstadosVencidos(userMail)

            // 2. Traer lista según filtro activo
            val clientes = if (filtroActual == null) {
                dao.obtenerTodos(userMail)
            } else {
                dao.obtenerPorEstado(filtroActual!!, userMail)
            }

            // 3. Cargar contadores de ventas para cada cliente
            val clientesConContadores = clientes.map { cliente ->
                val pagadas   = dao.contarVentasPagadas(cliente.id, userMail)
                val pendientes = dao.contarVentasPendientes(cliente.id, userMail)
                Triple(cliente, pagadas, pendientes)
            }

            clientesCargados.clear()
            clientesCargados.addAll(clientesConContadores)

            adapter.submitListConContadores(clientesConContadores)
        }
    }

    /**
     * Revisa si algún cliente tiene un estado transitorio (NUEVO_CLIENTE o PAGO_REALIZADO)
     * que ya venció sus 24hs y lo recalcula automáticamente.
     */
    private suspend fun recalcularEstadosVencidos(userMail: String) {
        val dao      = AppDatabase.getDatabase(requireContext()).clienteDao()
        val ahora    = System.currentTimeMillis()
        val limite   = ahora - EstadoCliente.DURACION_TRANSITORIO_MS

        // Caducado solo si fechaSeguimiento es ANTERIOR a hoy (no el mismo día)
        val inicioDehoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // — Estados transitorios vencidos (Nuevo Cliente / Pago Realizado de +24hs)
        val vencidos = dao.obtenerClientesConEstadoVencido(userMail, limite)
        vencidos.forEach { cliente ->
            val pendientes = dao.contarVentasPendientes(cliente.id, userMail)
            val nuevoEstado = when {
                pendientes > 0 -> EstadoCliente.PAGO_PENDIENTE
                else           -> EstadoCliente.NO_ASIGNADO
            }
            dao.update(cliente.copy(
                estado            = nuevoEstado,
                fechaCambioEstado = null
            ))
        }

        // — Caducados: ventas pendientes cuya fecha de seguimiento ya pasó
        val caducados = dao.obtenerClientesConSeguimientoVencido(userMail, inicioDehoy)
        caducados.forEach { cliente ->
            dao.update(cliente.copy(
                estado            = EstadoCliente.PAGO_CADUCADO,
                fechaCambioEstado = null
            ))
        }
    }

    private fun aplicarBusquedaYRender(query: String) {
        val texto = query.trim().lowercase()
        val filtrados = if (texto.isEmpty()) clientesCargados
        else clientesCargados.filter { (cliente, _, _) -> coincideBusqueda(cliente, texto) }
        adapter.submitListConContadores(filtrados)
    }

    private fun coincideBusqueda(cliente: Cliente, texto: String): Boolean =
        cliente.nombre.lowercase().contains(texto)   ||
                cliente.apellido.lowercase().contains(texto) ||
                cliente.email.lowercase().contains(texto)    ||
                cliente.telefono.lowercase().contains(texto) ||
                cliente.descripcion.lowercase().contains(texto)

    /* ========================================================================================
                                    DIÁLOGO: DETALLE DEL CLIENTE
       ======================================================================================== */

    /**
     * Se abre al presionar el botón flotante del borde inferior derecho del item.
     * Por ahora muestra los datos básicos del cliente.
     */
    private fun mostrarDialogDetalle(cliente: Cliente) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalle_cliente, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // — Nombre
        dialogView.findViewById<TextView>(R.id.tv_detalle_nombre).text =
            "${cliente.nombre} ${cliente.apellido}".trim()

        // — Estado con color dinámico
        val tvEstado = dialogView.findViewById<TextView>(R.id.tv_detalle_estado)
        tvEstado.text = cliente.estado

        EstadoColorHelper.aplicarBadgeCliente(requireContext(), tvEstado, cliente.estado)

        // — Datos
        dialogView.findViewById<TextView>(R.id.tv_detalle_email).text    = cliente.email
        dialogView.findViewById<TextView>(R.id.tv_detalle_telefono).text = cliente.telefono
        dialogView.findViewById<TextView>(R.id.tv_detalle_direccion).text =
            cliente.direccion.ifEmpty { "Sin dirección" }

        // — Fecha formateada
        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(cliente.fecha))
        dialogView.findViewById<TextView>(R.id.tv_detalle_fecha).text = fecha

        // — Descripción: mostrar solo si tiene texto
        val layoutDesc = dialogView.findViewById<View>(R.id.layout_descripcion)
        val tvDesc     = dialogView.findViewById<TextView>(R.id.tv_detalle_descripcion)
        if (cliente.descripcion.isNotEmpty()) {
            layoutDesc.visibility = View.VISIBLE
            tvDesc.text = cliente.descripcion
        }

        // — Botón Editar
        dialogView.findViewById<MaterialButton>(R.id.btn_detalle_editar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogEditar(cliente)
        }

        // — Botón Eliminar
        dialogView.findViewById<MaterialButton>(R.id.btn_detalle_eliminar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogEliminar(cliente)
        }

        // — Botón Cerrar
        dialogView.findViewById<MaterialButton>(R.id.btn_cerrar_detalle)
            .setOnClickListener { dialog.dismiss() }

        // — WhatsApp
        dialogView.findViewById<View>(R.id.btn_wsp).setOnClickListener {
            val numero = cliente.telefono.replace(Regex("[^0-9]"), "") // limpia caracteres
            val uri = Uri.parse("https://wa.me/$numero")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // — Email
        dialogView.findViewById<View>(R.id.btn_mail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${cliente.email}")
            }
            startActivity(Intent.createChooser(intent, "Enviar email"))
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /* ========================================================================================
                                    DIÁLOGO: NUEVO CLIENTE
       ======================================================================================== */

    private fun showNuevoClienteDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_nuevo_cliente)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val views = obtenerViewsNuevoCliente(dialog)

        views.btnGuardar.setOnClickListener  { procesarNuevoCliente(dialog, views) }
        views.btnCancelar.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun procesarNuevoCliente(dialog: Dialog, v: NuevoClienteViews) {
        val nombre    = v.nombre.text.toString().trim()
        val apellido  = v.apellido.text.toString().trim()
        val telefono  = v.telefono.text.toString().trim()
        val email     = v.email.text.toString().trim()
        val direccion = v.direccion.text.toString().trim()
        val desc      = v.descripcion.text.toString().trim()

        if (!validarCliente(nombre, telefono, email, v.tilNombre, v.tilTelefono, v.tilEmail)) return

        lifecycleScope.launch {
            val dao      = AppDatabase.getDatabase(requireContext()).clienteDao()
            val userMail = sessionManager.getUserMail()

            if (dao.obtenerPorEmail(email, userMail) != null) {
                v.tilEmail.error = "Email ya en uso"
                return@launch
            }

            val ahora = System.currentTimeMillis()

            dao.insert(Cliente(
                userMail          = userMail,
                nombre            = nombre,
                apellido          = apellido,
                telefono          = telefono,
                email             = email,
                direccion         = direccion,
                descripcion       = desc,
                estado            = EstadoCliente.NUEVO_CLIENTE,  // siempre automático
                fecha             = ahora,
                fechaCambioEstado = ahora                          // empieza el conteo de 24hs
            ))

            cargarClientes()
            Toast.makeText(requireContext(), "Cliente guardado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    /* ========================================================================================
                                    DIÁLOGO: EDITAR CLIENTE
       ======================================================================================== */

    private fun mostrarDialogEditar(cliente: Cliente) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_cliente, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val v = obtenerViewsEditar(view)

        // Precargar datos — el estado NO se edita manualmente
        v.nombre.setText(cliente.nombre)
        v.apellido.setText(cliente.apellido)
        v.telefono.setText(cliente.telefono)
        v.email.setText(cliente.email)
        v.direccion.setText(cliente.direccion)
        v.descripcion.setText(cliente.descripcion)

        // Limpiar errores al escribir
        v.nombre.doAfterTextChanged   { v.tilNombre.error = null }
        v.telefono.doAfterTextChanged { v.tilTelefono.error = null }
        v.email.doAfterTextChanged    { v.tilEmail.error = null }

        v.btnCancelar.setOnClickListener { dialog.dismiss() }

        v.btnGuardar.setOnClickListener {
            val nombre    = v.nombre.text.toString().trim()
            val apellido  = v.apellido.text.toString().trim()
            val telefono  = v.telefono.text.toString().trim()
            val email     = v.email.text.toString().trim()
            val direccion = v.direccion.text.toString().trim()
            val desc      = v.descripcion.text.toString().trim()

            if (!validarCliente(nombre, telefono, email, v.tilNombre, v.tilTelefono, v.tilEmail)) return@setOnClickListener

            lifecycleScope.launch {
                val dao      = AppDatabase.getDatabase(requireContext()).clienteDao()
                val userMail = sessionManager.getUserMail()

                if (email != cliente.email && dao.obtenerPorEmail(email, userMail) != null) {
                    v.tilEmail.error = "Email ya en uso"
                    return@launch
                }

                dao.update(cliente.copy(
                    nombre    = nombre,
                    apellido  = apellido,
                    telefono  = telefono,
                    email     = email,
                    direccion = direccion,
                    descripcion = desc
                    // estado NO se toca — es automático
                ))

                cargarClientes()
                Toast.makeText(requireContext(), "Cliente actualizado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /* ========================================================================================
                                    DIÁLOGO: ELIMINAR CLIENTE
       ======================================================================================== */

    private fun mostrarDialogEliminar(cliente: Cliente) {
        val view = layoutInflater.inflate(R.layout.dialog_eliminar_cliente, null)

        val dialog = Dialog(requireContext())
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        view.findViewById<TextView>(R.id.tv_titulo_eliminar).text =
            "¿Eliminar a ${cliente.nombre}?"

        view.findViewById<MaterialButton>(R.id.btn_eliminar_cliente).setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).clienteDao()
                    .marcarComoEliminado(cliente.id)
                cargarClientes()
                Toast.makeText(requireContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /* ========================================================================================
                                        VALIDACIONES
       ======================================================================================== */

    private fun validarCliente(
        nombre: String,
        telefono: String,
        email: String,
        tilNombre: TextInputLayout,
        tilTelefono: TextInputLayout,
        tilEmail: TextInputLayout
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

        return valido
    }

    /* ========================================================================================
                                        MENÚ FAB
       ======================================================================================== */

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
        fabOverlay.animate().alpha(0f).withEndAction { fabOverlay.visibility = View.GONE }.start()
        fabMenuContainer.animate().alpha(0f).translationY(20f).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()
        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }

    /* ========================================================================================
                                    DATA CLASSES DE VISTAS
       ======================================================================================== */

    data class NuevoClienteViews(
        val nombre: TextInputEditText,
        val apellido: TextInputEditText,
        val telefono: TextInputEditText,
        val email: TextInputEditText,
        val direccion: TextInputEditText,
        val descripcion: TextInputEditText,
        val tilNombre: TextInputLayout,
        val tilTelefono: TextInputLayout,
        val tilEmail: TextInputLayout,
        val btnGuardar: MaterialButton,
        val btnCancelar: MaterialButton
    )

    data class EditarClienteViews(
        val nombre: TextInputEditText,
        val apellido: TextInputEditText,
        val telefono: TextInputEditText,
        val email: TextInputEditText,
        val direccion: TextInputEditText,
        val descripcion: TextInputEditText,
        val tilNombre: TextInputLayout,
        val tilTelefono: TextInputLayout,
        val tilEmail: TextInputLayout,
        val btnGuardar: MaterialButton,
        val btnCancelar: MaterialButton
    )

    private fun obtenerViewsNuevoCliente(dialog: Dialog) = NuevoClienteViews(
        nombre      = dialog.findViewById(R.id.tiet_cliente_nombre),
        apellido    = dialog.findViewById(R.id.tiet_cliente_apellido),
        telefono    = dialog.findViewById(R.id.tiet_cliente_telefono),
        email       = dialog.findViewById(R.id.tiet_cliente_email),
        direccion   = dialog.findViewById(R.id.tiet_cliente_direccion),
        descripcion = dialog.findViewById(R.id.tiet_cliente_descripcion),
        tilNombre   = dialog.findViewById(R.id.til_cliente_nombre),
        tilTelefono = dialog.findViewById(R.id.til_cliente_telefono),
        tilEmail    = dialog.findViewById(R.id.til_cliente_email),
        btnGuardar  = dialog.findViewById(R.id.btn_guardar_cliente),
        btnCancelar = dialog.findViewById(R.id.btn_cancelar_cliente)
    )

    private fun obtenerViewsEditar(view: View) = EditarClienteViews(
        nombre      = view.findViewById(R.id.tiet_cliente_nombre),
        apellido    = view.findViewById(R.id.tiet_cliente_apellido),
        telefono    = view.findViewById(R.id.tiet_cliente_telefono),
        email       = view.findViewById(R.id.tiet_cliente_email),
        direccion   = view.findViewById(R.id.tiet_cliente_direccion),
        descripcion = view.findViewById(R.id.tiet_cliente_descripcion),
        tilNombre   = view.findViewById(R.id.til_cliente_nombre),
        tilTelefono = view.findViewById(R.id.til_cliente_telefono),
        tilEmail    = view.findViewById(R.id.til_cliente_email),
        btnGuardar  = view.findViewById(R.id.btn_guardar_cliente),
        btnCancelar = view.findViewById(R.id.btn_cancelar_cliente)
    )
}