package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

/* ---------- ANDROID CORE ---------- */
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/* ---------- WIDGETS / UI ---------- */
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

/* ---------- ANDROIDX ---------- */
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/* ---------- MATERIAL DESIGN ---------- */
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/* ---------- PROYECTO ---------- */
import com.followup.R
import com.followup.data.adapter.VentasAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.EstadoCliente
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager

/* ---------- FECHAS ---------- */
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ---------- CORRUTINAS ---------- */
import kotlinx.coroutines.launch
import java.text.DecimalFormat


class VentasFragment : Fragment() {

    /* ========================================================================================
                                        ESTADO DEL FRAGMENT
    ======================================================================================== */

    // --- FAB (Floating Action Button) ---
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenuContainer: View
    private lateinit var fabOverlay: View
    private lateinit var btnNuevaVenta: MaterialButton
    private var isFabMenuOpen = false

    // --- Lista de ventas ---
    private lateinit var recyclerVentas: RecyclerView
    private lateinit var adapter: VentasAdapter

    /** Copia sin filtrar de la lista traída desde la BD. Se usa como base para filtros y búsqueda. */
    private var listaOriginal: List<Venta> = listOf()

    // --- Filtros por estado ---
    private lateinit var btnFiltroEstado: MaterialCardView
    private lateinit var dropdownEstadosVenta: LinearLayout
    private var filtroActual: String? = null

    // --- Buscador ---
    private lateinit var inputBuscar: TextInputEditText

    // --- Sesión ---
    private lateinit var sessionManager: SessionManager

    /**
     * Formateador de fechas único para todo el Fragment.
     * Usar una sola instancia evita inconsistencias y objetos innecesarios.
     */
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))

    /* ========================================================================================
                                    CONSTANTES DE ESTADO
    ======================================================================================== */
    private object Estado {
        const val PENDIENTE   = "Pendiente"
        const val PAGADO      = "Pagado"
        const val NO_ASIGNADO = "No Asignado"
    }

    /* ========================================================================================
                                    DATA CLASSES INTERNAS
    ======================================================================================== */

    private data class ClienteOption(val id: Int, val nombre: String, val label: String)

    private data class VentaFormData(
        val clienteId: Int,
        val nombreCliente: String,
        val montoTotal: Double,
        val pagoTotal: Double,
        val fechaVenta: Long,
        val fechaSeguimiento: Long,
        val descripcion: String
    )


    /* ========================================================================================
                                        CICLO DE VIDA
    ======================================================================================== */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ventas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        initComponents(view)
        initAdapter()
        initListeners()
        cargarVentas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }


    /* ========================================================================================
                                    INICIALIZACIÓN
    ======================================================================================== */

    /** Vincula todas las vistas del layout con sus variables. */
    private fun initComponents(view: View) {
        fabMain              = view.findViewById(R.id.fab_main)
        fabMenuContainer     = view.findViewById(R.id.fab_menu_container)
        fabOverlay           = view.findViewById(R.id.fab_overlay)
        btnNuevaVenta        = view.findViewById(R.id.btn_nueva_venta)
        recyclerVentas       = view.findViewById(R.id.rvVentas)
        btnFiltroEstado      = view.findViewById(R.id.btn_filtro_estado)
        dropdownEstadosVenta = view.findViewById(R.id.dropdown_estados_venta)
        inputBuscar          = view.findViewById(R.id.search)

        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false
    }

    /** Crea el adaptador y lo asigna al RecyclerView. */
    private fun initAdapter() {
        adapter = VentasAdapter(object : VentasAdapter.OnVentaClickListener {
            override fun onDeleteClick(venta: Venta)  = mostrarDialogoEliminar(venta)
            override fun onEditClick(venta: Venta)    = mostrarDialogoEditar(venta)
            override fun onDetalleClick(venta: Venta) = mostrarDialogDetalle(venta)
        })

        recyclerVentas.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerVentas.clipChildren  = false
        recyclerVentas.clipToPadding = false
        recyclerVentas.adapter       = adapter
    }

    /** Registra todos los listeners de botones, filtros y buscador. */
    private fun initListeners() {

        // FAB principal
        fabMain.setOnClickListener   { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevaVenta.setOnClickListener {
            closeFabMenu()
            showNuevaVentaDialog()
        }

        // Botón filtro: abre/cierra el dropdown
        btnFiltroEstado.setOnClickListener {
            dropdownEstadosVenta.visibility =
                if (dropdownEstadosVenta.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Opciones del dropdown
        requireView().findViewById<TextView>(R.id.filtro_venta_todos).setOnClickListener {
            filtroActual = null
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_pendiente).setOnClickListener {
            filtroActual = Estado.PENDIENTE
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_pagado).setOnClickListener {
            filtroActual = Estado.PAGADO
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_caducado).setOnClickListener {
            filtroActual = "Pago caducado"
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }

        // Buscador
        inputBuscar.doAfterTextChanged { filtrarVentas() }
    }


    /* ========================================================================================
                                    MOSTRAR DIALOG DETALLE
    ======================================================================================== */

    private fun mostrarDialogDetalle(venta: Venta) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalle_venta, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val moneyFormatter = DecimalFormat("#,##0.00")

        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_id).text          = "Venta #${venta.id}"
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_cliente).text      = venta.nombreCliente
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_monto).text        = "$${moneyFormatter.format(venta.montoTotal)}"
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_pago).text         = "$${moneyFormatter.format(venta.pagoTotal)}"
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_fecha).text        = dateFormatter.format(Date(venta.fechaVenta))
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_seguimiento).text  = dateFormatter.format(Date(venta.fechaSeguimiento))
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_descripcion).text  =
            venta.descripcion.ifEmpty { "Sin descripción" }

        val tvEstado = dialogView.findViewById<TextView>(R.id.tv_detalle_venta_estado)
        tvEstado.text = venta.estado
        val colorEstado = colorParaEstado(venta.estado)
        (tvEstado.background as? GradientDrawable)?.setColor(Color.parseColor(colorEstado))

        val porcentaje = ((venta.pagoTotal / venta.montoTotal) * 100).toInt().coerceIn(0, 100)
        dialogView.findViewById<ProgressBar>(R.id.progress_detalle_pago).progress   = porcentaje
        dialogView.findViewById<TextView>(R.id.tv_detalle_venta_porcentaje).text     = "$porcentaje%"

        dialogView.findViewById<MaterialButton>(R.id.btn_detalle_venta_editar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEditar(venta)
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_detalle_venta_eliminar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEliminar(venta)
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_cerrar_detalle_venta).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }


    /* ========================================================================================
                                    CARGA Y FILTRADO DE VENTAS
    ======================================================================================== */

    private fun cargarVentas() {
        lifecycleScope.launch {
            val userMail = sessionManager.getUserMail()
            listaOriginal = AppDatabase.getDatabase(requireContext())
                .ventaDao()
                .obtenerTodas(userMail)
            filtrarVentas()
        }
    }

    /**
     * Único punto de actualización del adaptador.
     * Combina búsqueda de texto + filtro de estado.
     */
    private fun filtrarVentas() {
        val query = inputBuscar.text.toString().lowercase().trim()

        val resultado = listaOriginal.filter { venta ->
            coincideConBusqueda(venta, query) && coincideConEstado(venta)
        }

        adapter.submitList(resultado)
    }

    /**
     * Búsqueda inteligente: detecta email (@), teléfono (dígitos) o nombre.
     */
    private fun coincideConBusqueda(venta: Venta, query: String): Boolean {
        if (query.isEmpty()) return true
        return when {
            query.contains("@")        -> venta.emailCliente?.lowercase()?.contains(query) == true
            query.any { it.isDigit() } -> venta.telefonoCliente?.contains(query) == true
            else                       -> venta.nombreCliente.lowercase().contains(query)
        }
    }

    /** Null = Todos → siempre true. Con valor, compara ignorando mayúsculas. */
    private fun coincideConEstado(venta: Venta): Boolean {
        val f = filtroActual ?: return true
        return venta.estado.equals(f, ignoreCase = true)
    }


    /* ========================================================================================
                                    DIÁLOGO: EDITAR VENTA
    ======================================================================================== */

    private fun mostrarDialogoEditar(venta: Venta) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_venta, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        val montoTotal       = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_monto_total)
        val pagoTotal        = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_pago_total)
        val fechaVenta       = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_venta)
        val fechaSeguimiento = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_seguimiento)
        val descripcion      = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_descripcion)
        val btnCancelar      = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar_venta)
        val btnGuardar       = dialogView.findViewById<MaterialButton>(R.id.btn_guardar_venta)

        montoTotal.setText(venta.montoTotal.toString())
        pagoTotal.setText(venta.pagoTotal.toString())
        descripcion.setText(venta.descripcion)
        setupDateField(fechaVenta, venta.fechaVenta)
        setupDateField(fechaSeguimiento, venta.fechaSeguimiento)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val monto                = montoTotal.text.toString().toDoubleOrNull()
            val pago                 = pagoTotal.text.toString().toDoubleOrNull()
            val fechaVentaLong       = fechaVenta.tag as? Long
            val fechaSeguimientoLong = fechaSeguimiento.tag as? Long
            val desc                 = descripcion.text.toString()

            if (monto == null || monto <= 0) {
                Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pago == null || pago < 0) {
                Toast.makeText(requireContext(), "Pago inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pago > monto) {
                Toast.makeText(requireContext(), "El pago no puede ser mayor al monto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fechaVentaLong == null || fechaSeguimientoLong == null) {
                Toast.makeText(requireContext(), "Seleccioná las fechas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (desc.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresá una descripción (obligatorio)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val estado = if (Math.abs(pago - monto) < 0.001) Estado.PAGADO else Estado.PENDIENTE

            val ventaActualizada = venta.copy(
                montoTotal       = monto,
                pagoTotal        = pago,
                fechaVenta       = fechaVentaLong,
                fechaSeguimiento = fechaSeguimientoLong,
                descripcion      = desc,
                estado           = estado
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).ventaDao().update(ventaActualizada)
                actualizarEstadoCliente(venta.idClienteVenta)
                dialog.dismiss()
                cargarVentas()
            }
        }
    }


    /* ========================================================================================
                                    DIÁLOGO: ELIMINAR VENTA
    ======================================================================================== */

    private fun mostrarDialogoEliminar(venta: Venta) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar venta")
            .setMessage("¿Estás seguro que querés eliminar esta venta?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.ventaDao().softDelete(venta.id)
                    actualizarEstadoCliente(venta.idClienteVenta)
                    cargarVentas()
                    Toast.makeText(requireContext(), "Venta movida al historial", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    /* ========================================================================================
                                    DIÁLOGO: NUEVA VENTA
    ======================================================================================== */

    private fun showNuevaVentaDialog() {
        lifecycleScope.launch {
            val clientes = obtenerClientesDisponibles()

            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_nueva_venta)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            configurarFormularioVenta(
                findView            = { id -> dialog.findViewById(id) },
                clientesDisponibles = clientes,
                onCancelar          = { dialog.dismiss() },
                onGuardar           = { form -> registrarVenta(form, dialog) }
            )

            dialog.show()
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private suspend fun obtenerClientesDisponibles(): List<ClienteOption> {
        val userMail = sessionManager.getUserMail()
        return AppDatabase.getDatabase(requireContext())
            .clienteDao()
            .obtenerTodos(userMail)
            .map { cliente: Cliente ->
                ClienteOption(cliente.id, cliente.nombre, "${cliente.nombre} - ${cliente.email}")
            }
    }

    private fun configurarFormularioVenta(
        findView: (Int) -> View?,
        clientesDisponibles: List<ClienteOption>,
        onCancelar: () -> Unit,
        onGuardar: (VentaFormData) -> Unit
    ) {
        val tilCliente           = findView(R.id.til_venta_cliente)          as TextInputLayout
        val actvCliente          = findView(R.id.actv_venta_cliente)          as AutoCompleteTextView
        val tilMontoTotal        = findView(R.id.til_venta_monto_total)       as TextInputLayout
        val tietMontoTotal       = findView(R.id.tiet_venta_monto_total)      as TextInputEditText
        val tilPagoTotal         = findView(R.id.til_venta_pago_total)        as TextInputLayout
        val tietPagoTotal        = findView(R.id.tiet_venta_pago_total)       as TextInputEditText
        val tilFechaVenta        = findView(R.id.til_venta_fecha_venta)       as TextInputLayout
        val tietFechaVenta       = findView(R.id.tiet_venta_fecha_venta)      as TextInputEditText
        val tilFechaSeguimiento  = findView(R.id.til_venta_fecha_seguimiento) as TextInputLayout
        val tietFechaSeguimiento = findView(R.id.tiet_venta_fecha_seguimiento) as TextInputEditText
        val tilDescripcion       = findView(R.id.til_venta_descripcion)       as TextInputLayout
        val tietDescripcion      = findView(R.id.tiet_venta_descripcion)      as TextInputEditText
        val btnCancelar          = findView(R.id.btn_cancelar_venta)          as MaterialButton
        val btnGuardar           = findView(R.id.btn_guardar_venta)           as MaterialButton

        val clienteMap = clientesDisponibles.associateBy { it.label }

        actvCliente.keyListener = null
        actvCliente.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                clientesDisponibles.map { it.label }
            )
        )

        val ahora = System.currentTimeMillis()
        setupDateField(tietFechaVenta, ahora)
        setupDateField(tietFechaSeguimiento, ahora)

        if (clientesDisponibles.isEmpty()) {
            tilCliente.error = "No hay clientes disponibles"
            btnGuardar.isEnabled = false
        }

        actvCliente.doAfterTextChanged          { tilCliente.error = null }
        tietMontoTotal.doAfterTextChanged       { tilMontoTotal.error = null }
        tietPagoTotal.doAfterTextChanged        { tilPagoTotal.error = null }
        tietFechaVenta.doAfterTextChanged       { tilFechaVenta.error = null }
        tietFechaSeguimiento.doAfterTextChanged { tilFechaSeguimiento.error = null }
        tietDescripcion.doAfterTextChanged      { tilDescripcion.error = null }

        btnCancelar.setOnClickListener { onCancelar() }

        btnGuardar.setOnClickListener {
            val result = validarVentaFrontend(
                clienteSeleccionado = actvCliente.text?.toString()?.trim().orEmpty(),
                montoText           = tietMontoTotal.text?.toString()?.trim().orEmpty(),
                pagoText            = tietPagoTotal.text?.toString()?.trim().orEmpty(),
                fechaVenta          = tietFechaVenta.tag as? Long,
                fechaSeguimiento    = tietFechaSeguimiento.tag as? Long,
                descripcion         = tietDescripcion.text?.toString()?.trim().orEmpty(),
                clienteMap          = clienteMap,
                tilCliente          = tilCliente,
                tilMontoTotal       = tilMontoTotal,
                tilPagoTotal        = tilPagoTotal,
                tilFechaVenta       = tilFechaVenta,
                tilFechaSeguimiento = tilFechaSeguimiento,
                tilDescripcion      = tilDescripcion
            )
            if (result != null) onGuardar(result)
        }
    }


    /* ========================================================================================
                                    VALIDACIÓN DEL FORMULARIO
    ======================================================================================== */

    private fun validarVentaFrontend(
        clienteSeleccionado: String,
        montoText: String,
        pagoText: String,
        fechaVenta: Long?,
        fechaSeguimiento: Long?,
        descripcion: String,
        clienteMap: Map<String, ClienteOption>,
        tilCliente: TextInputLayout,
        tilMontoTotal: TextInputLayout,
        tilPagoTotal: TextInputLayout,
        tilFechaVenta: TextInputLayout,
        tilFechaSeguimiento: TextInputLayout,
        tilDescripcion: TextInputLayout
    ): VentaFormData? {

        var esValido = true

        val cliente = clienteMap[clienteSeleccionado]
        if (cliente == null) { tilCliente.error = "Seleccioná un cliente válido"; esValido = false }
        else tilCliente.error = null

        val monto = montoText.toDoubleOrNull()
        if (monto == null || monto <= 0.0) { tilMontoTotal.error = "Ingresá un monto válido"; esValido = false }
        else tilMontoTotal.error = null

        val pago = pagoText.toDoubleOrNull()
        if (pago == null || pago < 0.0) { tilPagoTotal.error = "Ingresá un pago válido"; esValido = false }
        else if (monto != null && pago > monto) { tilPagoTotal.error = "El pago no puede superar el monto"; esValido = false }
        else tilPagoTotal.error = null

        if (fechaVenta == null) { tilFechaVenta.error = "Seleccioná la fecha de venta"; esValido = false }
        else tilFechaVenta.error = null

        if (fechaSeguimiento == null) { tilFechaSeguimiento.error = "Seleccioná la fecha de seguimiento"; esValido = false }
        else tilFechaSeguimiento.error = null

        if (descripcion.isEmpty()) { tilDescripcion.error = "Ingresá una descripción"; esValido = false }
        else tilDescripcion.error = null

        if (!esValido || cliente == null || monto == null || pago == null ||
            fechaVenta == null || fechaSeguimiento == null) return null

        return VentaFormData(
            clienteId        = cliente.id,
            nombreCliente    = cliente.nombre,
            montoTotal       = monto,
            pagoTotal        = pago,
            fechaVenta       = fechaVenta,
            fechaSeguimiento = fechaSeguimiento,
            descripcion      = descripcion
        )
    }


    /* ========================================================================================
                                    REGISTRAR NUEVA VENTA
    ======================================================================================== */

    private fun registrarVenta(form: VentaFormData, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val db       = AppDatabase.getDatabase(requireContext())
                val userMail = sessionManager.getUserMail()

                val cliente = db.clienteDao()
                    .obtenerTodos(userMail)
                    .find { it.id == form.clienteId }

                val estado = if (Math.abs(form.pagoTotal - form.montoTotal) < 0.001) Estado.PAGADO
                else Estado.PENDIENTE

                val venta = Venta(
                    idClienteVenta   = form.clienteId,
                    nombreCliente    = form.nombreCliente,
                    userMail         = userMail,
                    emailCliente     = cliente?.email.orEmpty(),
                    telefonoCliente  = cliente?.telefono.orEmpty(),
                    montoTotal       = form.montoTotal,
                    pagoTotal        = form.pagoTotal,
                    fechaVenta       = form.fechaVenta,
                    fechaSeguimiento = form.fechaSeguimiento,
                    descripcion      = form.descripcion,
                    total            = form.montoTotal,
                    fecha            = form.fechaVenta,
                    formaPago        = "Pago total",
                    estado           = estado
                )

                db.ventaDao().insert(venta)
                actualizarEstadoCliente(form.clienteId)
                cargarVentas()

                Toast.makeText(requireContext(), "Venta guardada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

            } catch (e: Exception) {
                android.util.Log.e("VENTAS_ERROR", "Error al guardar venta: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    /* ========================================================================================
                                    ACTUALIZAR ESTADO DEL CLIENTE
    ======================================================================================== */

    private suspend fun actualizarEstadoCliente(clienteId: Int) {
        val db       = AppDatabase.getDatabase(requireContext())
        val userMail = sessionManager.getUserMail()

        val cliente = db.clienteDao().obtenerPorId(clienteId) ?: return

        val caducadas  = db.clienteDao().contarVentasCaducadas(clienteId, userMail)
        val pendientes = db.clienteDao().contarVentasPendientes(clienteId, userMail)
        val pagadas    = db.clienteDao().contarVentasPagadas(clienteId, userMail)

        // Sin ninguna venta activa → No Asignado, siempre, sin excepciones
        if (caducadas == 0 && pendientes == 0 && pagadas == 0) {
            db.clienteDao().update(cliente.copy(
                estado            = EstadoCliente.NO_ASIGNADO,
                fechaCambioEstado = null
            ))
            return
        }

        // Prioridad: Caducado > Pendiente > Pagado (PAGO_REALIZADO transitorio)
        val nuevoEstado = when {
            caducadas  > 0 -> EstadoCliente.PAGO_CADUCADO
            pendientes > 0 -> EstadoCliente.PAGO_PENDIENTE
            else           -> EstadoCliente.PAGO_REALIZADO  // solo pagadas
        }

        // PAGO_REALIZADO necesita timestamp para el ciclo de 24hs
        val fechaCambio = if (nuevoEstado == EstadoCliente.PAGO_REALIZADO)
            System.currentTimeMillis() else null

        // Evitar writes innecesarios si el estado no cambió
        // (excepto PAGO_REALIZADO que siempre renueva el timestamp)
        if (cliente.estado == nuevoEstado && nuevoEstado != EstadoCliente.PAGO_REALIZADO) return

        db.clienteDao().update(cliente.copy(
            estado            = nuevoEstado,
            fechaCambioEstado = fechaCambio
        ))
    }


    /* ========================================================================================
                                    SELECTOR DE FECHA (DATEPICKER)
    ======================================================================================== */

    private fun setupDateField(field: TextInputEditText, initialMillis: Long) {
        field.tag = initialMillis
        field.setText(dateFormatter.format(initialMillis))

        field.setOnClickListener {
            val cal = Calendar.getInstance().apply {
                timeInMillis = field.tag as? Long ?: System.currentTimeMillis()
            }
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    field.tag = selected
                    field.setText(dateFormatter.format(selected))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }


    /* ========================================================================================
                                        MENÚ FAB
    ======================================================================================== */

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        isFabMenuOpen = true

        fabOverlay.visibility       = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE
        fabOverlay.isClickable      = true
        fabOverlay.isFocusable      = true

        fabOverlay.bringToFront()
        fabMenuContainer.bringToFront()
        fabMain.bringToFront()

        fabOverlay.alpha = 0f
        fabOverlay.animate().alpha(1f).setDuration(150).start()

        fabMenuContainer.alpha        = 0f
        fabMenuContainer.translationY = 20f
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(180).start()

        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

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


    /* ========================================================================================
                                    UTILIDADES
    ======================================================================================== */

    /** Mapa centralizado de estado → color hex. Usado en el adaptador y en el diálogo de detalle. */
    private fun colorParaEstado(estado: String): String = when (estado.lowercase()) {
        "pendiente"     -> "#F79009"
        "pagado"        -> "#12B76A"
        "pago caducado" -> "#F04438"
        else            -> "#98A2B3"
    }
}