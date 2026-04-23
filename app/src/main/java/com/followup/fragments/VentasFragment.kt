package com.followup.fragments

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

/* ---------- ANDROID CORE ---------- */
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/* ---------- WIDGETS / UI ---------- */
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast

/* ---------- ANDROIDX ---------- */
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/* ---------- MATERIAL DESIGN ---------- */
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/* ---------- PROYECTO ---------- */
import com.followup.R
import com.followup.data.adapter.VentasAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager

/* ---------- FECHAS ---------- */
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ---------- CORRUTINAS ---------- */
import kotlinx.coroutines.launch


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
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var btnFilterTodos: MaterialButton
    private lateinit var btnFilterVendidos: MaterialButton
    private lateinit var btnFilterPendiente: MaterialButton

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
    ========================================================================================
       Centralizar los strings de estado evita errores de tipeo en comparaciones.
    */
    private object Estado {
        const val PAGADO      = "Pagado"
        const val PENDIENTE   = "Pendiente"
        const val NO_ASIGNADO = "No Asignado"
    }

    /* ========================================================================================
                                    DATA CLASSES INTERNAS
    ======================================================================================== */

    /* Opción del dropdown de clientes en el formulario de nueva venta. */
    private data class ClienteOption(val id: Int, val nombre: String, val label: String)

    /* Datos del formulario ya validados, listos para construir una entidad Venta. */
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

        // Seleccionar "Todos" como filtro inicial
        btnFilterTodos.isChecked = true
        actualizarEstilosFiltro(R.id.btn_filter_todos)
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
        fabMain            = view.findViewById(R.id.fab_main)
        fabMenuContainer   = view.findViewById(R.id.fab_menu_container)
        fabOverlay         = view.findViewById(R.id.fab_overlay)
        btnNuevaVenta      = view.findViewById(R.id.btn_nueva_venta)
        recyclerVentas     = view.findViewById(R.id.rvVentas)
        filterGroup        = view.findViewById(R.id.filter_group)
        btnFilterTodos     = view.findViewById(R.id.btn_filter_todos)
        btnFilterVendidos  = view.findViewById(R.id.btn_filter_vendidos)
        btnFilterPendiente = view.findViewById(R.id.btn_filter_pendiente)
        inputBuscar        = view.findViewById(R.id.search)

        // El overlay empieza no interactuable; se activa solo cuando el FAB está abierto
        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false
    }

    /* Crea el adaptador y lo asigna al RecyclerView. */
    private fun initAdapter() {
        adapter = VentasAdapter(object : VentasAdapter.OnVentaClickListener {
            override fun onDeleteClick(venta: Venta) = mostrarDialogoEliminar(venta)
            override fun onEditClick(venta: Venta)   = mostrarDialogoEditar(venta)
        })
        recyclerVentas.layoutManager = LinearLayoutManager(requireContext())
        recyclerVentas.adapter = adapter
    }

    /* Registra todos los listeners de botones, filtros y buscador. */
    private fun initListeners() {

        // FAB principal abre/cierra el menú; el overlay lo cierra al tocar fuera
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevaVenta.setOnClickListener {
            closeFabMenu()
            showNuevaVentaDialog()
        }

        // Filtros por estado: al cambiar el botón activo se re-filtra la lista
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            actualizarEstilosFiltro(checkedId)
            filtrarVentas()  // único punto que aplica filtro + búsqueda
        }

        // Buscador: filtra por nombre, email o teléfono al escribir
        inputBuscar.doAfterTextChanged { filtrarVentas() }
    }


    /* ========================================================================================
                                    CARGA Y FILTRADO DE VENTAS
       ======================================================================================== */

    /**
     * Trae todas las ventas del usuario desde la BD y guarda la lista original.
     * Después aplica los filtros vigentes para actualizar el RecyclerView.
     */
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
     * Filtra [listaOriginal] combinando el estado del botón activo y el texto del buscador.
     * Es el único lugar donde se llama a [adapter.submitList], evitando duplicaciones.
     */
    private fun filtrarVentas() {
        val query      = inputBuscar.text.toString().lowercase().trim()
        val selectedId = filterGroup.checkedButtonId

        val resultado = listaOriginal.filter { venta ->
            coincideConBusqueda(venta, query) && coincideConEstado(venta, selectedId)
        }

        adapter.submitList(resultado)
    }

    /**
     * Devuelve true si la venta coincide con el texto buscado.
     * Detecta automáticamente si se busca por email (contiene @), teléfono (contiene dígitos)
     * o nombre (cualquier otro texto).
     */
    private fun coincideConBusqueda(venta: Venta, query: String): Boolean {
        if (query.isEmpty()) return true
        return when {
            query.contains("@") ->
                venta.emailCliente?.lowercase()?.contains(query) == true
            query.any { it.isDigit() } ->
                venta.telefonoCliente?.contains(query) == true
            else ->
                venta.nombreCliente.lowercase().contains(query)
        }
    }

    /**
     * Devuelve true si la venta coincide con el filtro de estado seleccionado.
     * Si el botón activo es "Todos", siempre devuelve true.
     */
    private fun coincideConEstado(venta: Venta, selectedId: Int): Boolean {
        return when (selectedId) {
            R.id.btn_filter_vendidos  -> venta.estado.equals(Estado.PAGADO,    ignoreCase = true)
            R.id.btn_filter_pendiente -> venta.estado.equals(Estado.PENDIENTE, ignoreCase = true)
            else                      -> true
        }
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

        // Referencias a campos del formulario
        val montoTotal       = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_monto_total)
        val pagoTotal        = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_pago_total)
        val fechaVenta       = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_venta)
        val fechaSeguimiento = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_seguimiento)
        val descripcion      = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_descripcion)
        val btnCancelar      = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar_venta)
        val btnGuardar       = dialogView.findViewById<MaterialButton>(R.id.btn_guardar_venta)

        // Precargar datos existentes de la venta
        montoTotal.setText(venta.montoTotal.toString())
        pagoTotal.setText(venta.pagoTotal.toString())
        descripcion.setText(venta.descripcion)

        // Precargar fechas usando el dateFormatter único del Fragment
        setupDateField(fechaVenta, venta.fechaVenta)
        setupDateField(fechaSeguimiento, venta.fechaSeguimiento)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {

            val monto = montoTotal.text.toString().toDoubleOrNull()
            val pago  = pagoTotal.text.toString().toDoubleOrNull()
            val fechaVentaLong       = fechaVenta.tag as? Long
            val fechaSeguimientoLong = fechaSeguimiento.tag as? Long
            val desc = descripcion.text.toString()

            // Validaciones — cada una muestra su propio mensaje de error
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
                Toast.makeText(requireContext(), "Ingresá una descripción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Determinar estado con tolerancia para evitar errores de punto flotante con Double
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
                AppDatabase.getDatabase(requireContext())
                    .ventaDao()
                    .update(ventaActualizada)

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

    /** Obtiene los clientes del usuario y los convierte en opciones para el dropdown. */
    private suspend fun obtenerClientesDisponibles(): List<ClienteOption> {
        val userMail = sessionManager.getUserMail()
        return AppDatabase.getDatabase(requireContext())
            .clienteDao()
            .obtenerTodos(userMail)
            .map { cliente: Cliente ->
                ClienteOption(cliente.id, cliente.nombre, "${cliente.nombre} - ${cliente.email}")
            }
    }

    /**
     * Configura todos los campos, validaciones y botones del formulario de venta.
     * Recibe lambdas para cancelar y guardar, lo que lo hace reutilizable para
     * distintos diálogos sin duplicar código.
     */
    private fun configurarFormularioVenta(
        findView: (Int) -> View?,
        clientesDisponibles: List<ClienteOption>,
        onCancelar: () -> Unit,
        onGuardar: (VentaFormData) -> Unit
    ) {
        val tilCliente           = findView(R.id.til_venta_cliente) as TextInputLayout
        val actvCliente          = findView(R.id.actv_venta_cliente) as AutoCompleteTextView
        val tilMontoTotal        = findView(R.id.til_venta_monto_total) as TextInputLayout
        val tietMontoTotal       = findView(R.id.tiet_venta_monto_total) as TextInputEditText
        val tilPagoTotal         = findView(R.id.til_venta_pago_total) as TextInputLayout
        val tietPagoTotal        = findView(R.id.tiet_venta_pago_total) as TextInputEditText
        val tilFechaVenta        = findView(R.id.til_venta_fecha_venta) as TextInputLayout
        val tietFechaVenta       = findView(R.id.tiet_venta_fecha_venta) as TextInputEditText
        val tilFechaSeguimiento  = findView(R.id.til_venta_fecha_seguimiento) as TextInputLayout
        val tietFechaSeguimiento = findView(R.id.tiet_venta_fecha_seguimiento) as TextInputEditText
        val tilDescripcion       = findView(R.id.til_venta_descripcion) as TextInputLayout
        val tietDescripcion      = findView(R.id.tiet_venta_descripcion) as TextInputEditText
        val btnCancelar          = findView(R.id.btn_cancelar_venta) as MaterialButton
        val btnGuardar           = findView(R.id.btn_guardar_venta) as MaterialButton

        // Mapa para buscar el ClienteOption seleccionado a partir del label del dropdown
        val clienteMap = clientesDisponibles.associateBy { it.label }

        // Dropdown de clientes (solo lectura, no se puede tipear libremente)
        actvCliente.keyListener = null
        actvCliente.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                clientesDisponibles.map { it.label }
            )
        )

        // Inicializar fechas con el momento actual
        val ahora = System.currentTimeMillis()
        setupDateField(tietFechaVenta, ahora)
        setupDateField(tietFechaSeguimiento, ahora)

        // Si no hay clientes, mostrar error y deshabilitar el botón guardar
        if (clientesDisponibles.isEmpty()) {
            tilCliente.error = "No hay clientes disponibles"
            btnGuardar.isEnabled = false
        }

        // Limpiar el error de cada campo en cuanto el usuario empieza a escribir
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

    /**
     * Valida todos los campos del formulario de nueva venta.
     * Muestra errores inline debajo de cada campo usando TextInputLayout.
     *
     * @return [VentaFormData] con los datos listos si todo es válido, o null si hay errores.
     */
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
        if (cliente == null) {
            tilCliente.error = "Seleccioná un cliente válido"
            esValido = false
        } else tilCliente.error = null

        val monto = montoText.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            tilMontoTotal.error = "Ingresá un monto válido"
            esValido = false
        } else tilMontoTotal.error = null

        val pago = pagoText.toDoubleOrNull()
        if (pago == null || pago < 0.0) {
            tilPagoTotal.error = "Ingresá un pago válido"
            esValido = false
        } else if (monto != null && pago > monto) {
            tilPagoTotal.error = "El pago no puede superar el monto"
            esValido = false
        } else tilPagoTotal.error = null

        if (fechaVenta == null) {
            tilFechaVenta.error = "Seleccioná la fecha de venta"
            esValido = false
        } else tilFechaVenta.error = null

        if (fechaSeguimiento == null) {
            tilFechaSeguimiento.error = "Seleccioná la fecha de seguimiento"
            esValido = false
        } else tilFechaSeguimiento.error = null

        if (descripcion.isEmpty()) {
            tilDescripcion.error = "Ingresá una descripción"
            esValido = false
        } else tilDescripcion.error = null

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

    /** Construye la entidad Venta a partir del formulario validado y la persiste en la BD. */
    private fun registrarVenta(form: VentaFormData, dialog: Dialog) {
        lifecycleScope.launch {
            val db       = AppDatabase.getDatabase(requireContext())
            val userMail = sessionManager.getUserMail()

            // Buscar el cliente para obtener email y teléfono (necesarios para el buscador)
            val cliente = db.clienteDao()
                .obtenerTodos(userMail)
                .find { it.id == form.clienteId }

            val estado = if (form.pagoTotal >= form.montoTotal) Estado.PAGADO else Estado.PENDIENTE

            val venta = Venta(
                idClienteVenta   = form.clienteId,
                nombreCliente    = form.nombreCliente,
                userMail         = userMail,
                emailCliente     = cliente?.email ?: "",
                telefonoCliente  = cliente?.telefono ?: "",
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

            Toast.makeText(requireContext(), "Venta guardada con éxito", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }


    /* ========================================================================================
                                    ACTUALIZAR ESTADO DEL CLIENTE
       ======================================================================================== */

    /**
     * Recalcula el estado del cliente según sus ventas activas y lo persiste.
     *
     * Reglas:
     *  - Sin ventas activas  → "No Asignado"
     *  - Alguna pendiente    → "Pendiente"
     *  - Todas pagadas       → "Vendido"
     */
    private suspend fun actualizarEstadoCliente(clienteId: Int) {
        val db       = AppDatabase.getDatabase(requireContext())
        val userMail = sessionManager.getUserMail()

        val estados = db.ventaDao().obtenerEstadosPorCliente(clienteId, userMail)

        val nuevoEstado = when {
            estados.isEmpty()                                                  -> Estado.NO_ASIGNADO
            estados.any { it.equals(Estado.PENDIENTE, ignoreCase = true) }    -> Estado.PENDIENTE
            else                                                               -> "Vendido"
        }

        val cliente = db.clienteDao()
            .obtenerTodos(userMail)
            .find { it.id == clienteId } ?: return

        db.clienteDao().update(cliente.copy(estado = nuevoEstado))
    }


    /* ========================================================================================
                                    SELECTOR DE FECHA (DATEPICKER)
    ======================================================================================== */

    /**
     * Configura un campo de texto para mostrar un DatePickerDialog al tocarlo.
     * El timestamp seleccionado se guarda en el [tag] del campo para recuperarlo después.
     *
     * @param field         Campo que actuará como selector de fecha.
     * @param initialMillis Fecha inicial a mostrar (en milisegundos).
     */
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
                               ESTILOS DE LOS BOTONES DE FILTRO
    ======================================================================================== */

    /**
     * Actualiza el aspecto visual de los botones de filtrado.
     * El botón activo se pinta con azul; los demás quedan en blanco.
     */
    private fun actualizarEstilosFiltro(selectedId: Int) {
        val botones = listOf(btnFilterTodos, btnFilterVendidos, btnFilterPendiente)

        botones.forEach { btn ->
            if (btn.id == selectedId) {
                btn.setBackgroundColor(Color.parseColor("#286DFF"))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.WHITE)
                btn.setTextColor(Color.parseColor("#475467"))
            }
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

        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE
        fabOverlay.isClickable = true
        fabOverlay.isFocusable = true

        // Traer al frente para que el overlay tape el contenido de abajo
        fabOverlay.bringToFront()
        fabMenuContainer.bringToFront()
        fabMain.bringToFront()

        // Animar entrada del overlay (fade in)
        fabOverlay.alpha = 0f
        fabOverlay.animate().alpha(1f).setDuration(150).start()

        // Animar entrada del menú (fade in + slide up)
        fabMenuContainer.alpha = 0f
        fabMenuContainer.translationY = 20f
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(180).start()

        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false

        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false

        // Animar salida del overlay (fade out → ocultar)
        fabOverlay.animate().alpha(0f).setDuration(120).withEndAction {
            fabOverlay.visibility = View.GONE
        }.start()

        // Animar salida del menú (fade out + slide down → ocultar)
        fabMenuContainer.animate().alpha(0f).translationY(20f).setDuration(140).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()

        fabMain.bringToFront()
        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }
}