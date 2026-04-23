package com.followup.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.compose.material3.FloatingActionButton
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.VentasAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.Date

class VentasFragment : Fragment() {
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenuContainer: View
    private lateinit var fabOverlay: View
    private lateinit var btnNuevaVenta: MaterialButton
    private var isFabMenuOpen = false

    private lateinit var recyclerVentas: RecyclerView // RECYCLERVIEW
    private lateinit var adapter: VentasAdapter // ADAPTADOR

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))

    private data class ClienteOption(val id: Int, val nombre: String, val label: String)

    // VARIABLES DE LA VISTA VENTAS ( BOTONES DE FILTRADOS )
    private lateinit var filterGroup: MaterialButtonToggleGroup // GRUPO DE LOS BOTONES
    private lateinit var btnFilterTodos: MaterialButton // BOTÓN TODOS
    private lateinit var btnFilterVendidos: MaterialButton // BOTÓN VENDIDOS
    private lateinit var btnFilterPendiente: MaterialButton // BOTÓN PENDIENTE

    private var listaOriginal: List<Venta> = listOf() // LISTA ORIGINAL DE VENTAS

    // BUSCADOR PARA FILTRAR VENTAS POR [ NOMBRE, MAIL, TELÉFONO ]
    private lateinit var inputBuscar: TextInputEditText

    private lateinit var sessionManager: SessionManager

    private data class VentaFormData(
        val clienteId: Int,
        val nombreCliente: String,
        val montoTotal: Double,
        val pagoTotal: Double,
        val fechaVenta: Long,
        val fechaSeguimiento: Long,
        val descripcion: String
    )

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
        initListeners()

        // CONFIGURAR EL ADAPTADOR
        adapter = VentasAdapter(object : VentasAdapter.OnVentaClickListener {

            // DISPARADOR QUE SE EJECUTA CUANDO SE HACE CLICK EN EL BOTÓN DE ELIMINAR VENTA
            override fun onDeleteClick(venta: Venta) {
                mostrarDialogoEliminar(venta)
            }

            // DISPARADOR QUE SE EJECUTA CUANDO SE HACE CLICK EN EL BOTÓN DE EDITAR VENTA
            override fun onEditClick(venta: Venta) {
                mostrarDialogoEditar(venta) // MUESTRA EL DIALOGO DE EDICIÓN
            }

        })

        recyclerVentas.layoutManager = LinearLayoutManager(requireContext())
        recyclerVentas.adapter = adapter

        cargarVentas()

        // SELECCIONAR EL PRIMER BOTÓN DE FILTRADO Y DARLE EL ESTILO
        btnFilterTodos.isChecked = true
        actualizarEstilosFiltro(R.id.btn_filter_todos)

    }

    /* --------------------------------------------------
                        CARGAR VENTAS
    -------------------------------------------------- */
    private fun cargarVentas() {
        lifecycleScope.launch {

            val userMail = sessionManager.getUserMail()

            val ventas = AppDatabase.getDatabase(requireContext())
                .ventaDao()
                .obtenerTodas(userMail)

            listaOriginal = ventas

            val selectedId = filterGroup.checkedButtonId

            val listaFiltrada = when (selectedId) {
                R.id.btn_filter_vendidos -> ventas.filter {
                    it.estado.equals("Pagado", true)
                }
                R.id.btn_filter_pendiente -> ventas.filter {
                    it.estado.equals("Pendiente", true)
                }
                else -> ventas
            }

            adapter.submitList(listaFiltrada)
        }
    }

    /* --------------------------------------------------
                    MOSTRAR DIALOG EDITAR
    -------------------------------------------------- */

    private fun mostrarDialogoEditar(venta: Venta) {

        // CREAR EL DIALOG
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_venta, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        // REFERENCIAR CAMPOS
        val montoTotal = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_monto_total)
        val pagoTotal = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_pago_total)
        val fechaVenta = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_venta)
        val fechaSeguimiento = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_seguimiento)
        val descripcion = dialogView.findViewById<TextInputEditText>(R.id.tiet_venta_descripcion)

        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar_venta)
        val btnGuardar = dialogView.findViewById<MaterialButton>(R.id.btn_guardar_venta)

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // CARGA SEGURA
        fechaVenta.setText(formato.format(Date(venta.fechaVenta)))
        fechaVenta.tag = venta.fechaVenta

        fechaSeguimiento.setText(formato.format(Date(venta.fechaSeguimiento)))
        fechaSeguimiento.tag = venta.fechaSeguimiento

        montoTotal.setText(venta.montoTotal.toString())
        pagoTotal.setText(venta.pagoTotal.toString())
        descripcion.setText(venta.descripcion)

        // ABRIR DATAPICKER (PARA LA FECHA DE VENTA)
        setupDateField(fechaVenta, venta.fechaVenta)
        setupDateField(fechaSeguimiento, venta.fechaSeguimiento)

        // BOTÓN CANCELAR
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // BOTÓN GUARDAR
        btnGuardar.setOnClickListener {

            val monto = montoTotal.text.toString().toDoubleOrNull()
            val pago = pagoTotal.text.toString().toDoubleOrNull()

            val fechaVentaLong = fechaVenta.tag as? Long
            val fechaSeguimientoLong = fechaSeguimiento.tag as? Long

            val desc = descripcion.text.toString()

            // VALIDACIONES
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

            // ACTUALIZAR EL ESTADO CORRECTO
            val estado = when {
                pago < monto -> "Pendiente"
                pago == monto -> "Pagado"
                else -> "Error"
            }

            val ventaActualizada = venta.copy(
                montoTotal = monto,
                pagoTotal = pago,
                fechaVenta = fechaVentaLong,
                fechaSeguimiento = fechaSeguimientoLong,
                descripcion = desc,
                estado = estado
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext())
                    .ventaDao()
                    .update(ventaActualizada)

                actualizarEstadoCliente(venta.idClienteVenta) // 🔥 IMPORTANTE

                dialog.dismiss()
                cargarVentas()
            }
        }
    }

    /* --------------------------------------------------
                    MOSTRAR DIALOG ELIMINAR
    -------------------------------------------------- */
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

    private fun initComponents(view: View) {
        fabMain = view.findViewById(R.id.fab_main)
        fabMenuContainer = view.findViewById(R.id.fab_menu_container)
        fabOverlay = view.findViewById(R.id.fab_overlay)
        btnNuevaVenta = view.findViewById(R.id.btn_nueva_venta)

        fabOverlay.isClickable = false
        fabOverlay.isFocusable = false

        recyclerVentas = view.findViewById(R.id.rvVentas) // RECYCLERVIEW

        // COMPONENTES DE LA VISTA VENTAS ( BOTONES DE FILTRADOS )
        filterGroup = view.findViewById(R.id.filter_group)
        btnFilterTodos = view.findViewById(R.id.btn_filter_todos)
        btnFilterVendidos = view.findViewById(R.id.btn_filter_vendidos)
        btnFilterPendiente = view.findViewById(R.id.btn_filter_pendiente)

        // INICIALIZAR BUSCADOR
        inputBuscar = view.findViewById(R.id.search)
    }

    /* --------------------------------------------------
                INICIALIZADOR DE LISTENERS
    -------------------------------------------------- */
    private fun initListeners() {
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevaVenta.setOnClickListener {
            closeFabMenu()
            showNuevaVentaDialog()
        }

        // BOTONES DE FILTRADO [ FILTRA LISTA DE VENTAS POR ESTADO ]
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (!isChecked) return@addOnButtonCheckedListener

            actualizarEstilosFiltro(checkedId) // ACTUALIZA LOS ESTILOS DE LOS BOTONES

            filtrarVentas() // FILTRA LA LISTA DE VENTAS

            when (checkedId) {

                R.id.btn_filter_todos -> {
                    adapter.submitList(listaOriginal)
                }

                R.id.btn_filter_vendidos -> {
                    val filtrados = listaOriginal.filter {
                        it.estado.equals("Pagado", ignoreCase = true)
                    }
                    adapter.submitList(filtrados)
                }

                R.id.btn_filter_pendiente -> {
                    val filtrados = listaOriginal.filter {
                        it.estado.equals("Pendiente", ignoreCase = true)
                    }
                    adapter.submitList(filtrados)
                }
            }
        }

        // BUSCADOR DE VENTAS
            // [1] - SE FILTRA LA LISTA DE VENTAS CUANDO SE ESCRIBE EN EL BUSCADOR
        inputBuscar.doAfterTextChanged { texto ->
            filtrarVentas()
        }

    }

    /* --------------------------------------------------
                        FILTRAR VENTAS
    -------------------------------------------------- */
    private fun filtrarVentas() {

        val query = inputBuscar.text.toString().lowercase().trim()
        val selectedId = filterGroup.checkedButtonId

        val filtrados = listaOriginal.filter { venta ->

            val coincideBusqueda = when {

                // BUSCAR POR EMAIL
                query.contains("@") -> {
                    venta.emailCliente?.lowercase()?.contains(query) == true
                }

                // BUSCAR POR TELÉFONO
                query.any { it.isDigit() } -> {
                    venta.telefonoCliente?.contains(query) == true
                }

                // ✍BUSCAR POR NOMBRE
                else -> {
                    venta.nombreCliente.lowercase().contains(query)
                }
            }

            val coincideEstado = when (selectedId) {

                R.id.btn_filter_vendidos ->
                    venta.estado.equals("Pagado", true)

                R.id.btn_filter_pendiente ->
                    venta.estado.equals("Pendiente", true)

                else -> true
            }

            coincideBusqueda && coincideEstado
        }

        adapter.submitList(filtrados)
    }

    /* --------------------------------------------------
       ACTUALIZAR ESTILOS DE LOS BOTONES DE FILTRADO
    -------------------------------------------------- */

    private fun actualizarEstilosFiltro(selectedId: Int) {

        val botones = listOf(
            btnFilterTodos,
            btnFilterVendidos,
            btnFilterPendiente
        )

        botones.forEach { btn ->

            if (btn.id == selectedId) {
                // SELECCIONADO
                btn.setBackgroundColor(Color.parseColor("#286DFF"))
                btn.setTextColor(Color.WHITE)
            } else {
                // NO SELECCIONADO
                btn.setBackgroundColor(Color.WHITE)
                btn.setTextColor(Color.parseColor("#475467"))
            }
        }
    }

    private fun showNuevaVentaDialog() {
        lifecycleScope.launch {
            val clientes = obtenerClientesDisponibles()
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_nueva_venta)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            configurarFormularioVenta(
                findView = { id -> dialog.findViewById(id) },
                clientesDisponibles = clientes,
                onCancelar = { dialog.dismiss() },
                onGuardar = { form -> registrarVenta(form, dialog) }
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

        val clientes = AppDatabase.getDatabase(requireContext())
            .clienteDao()
            .obtenerTodos(userMail)

        return clientes.map { cliente: Cliente ->
            ClienteOption(cliente.id, cliente.nombre, "${cliente.nombre} - ${cliente.email}")
        }
    }

    private fun configurarFormularioVenta(
        findView: (Int) -> View?,
        clientesDisponibles: List<ClienteOption>,
        onCancelar: () -> Unit,
        onGuardar: (VentaFormData) -> Unit
    ) {
        val tilCliente = findView(R.id.til_venta_cliente) as TextInputLayout
        val actvCliente = findView(R.id.actv_venta_cliente) as AutoCompleteTextView
        val tilMontoTotal = findView(R.id.til_venta_monto_total) as TextInputLayout
        val tietMontoTotal = findView(R.id.tiet_venta_monto_total) as TextInputEditText
        val tilPagoTotal = findView(R.id.til_venta_pago_total) as TextInputLayout
        val tietPagoTotal = findView(R.id.tiet_venta_pago_total) as TextInputEditText
        val tilFechaVenta = findView(R.id.til_venta_fecha_venta) as TextInputLayout
        val tietFechaVenta = findView(R.id.tiet_venta_fecha_venta) as TextInputEditText
        val tilFechaSeguimiento = findView(R.id.til_venta_fecha_seguimiento) as TextInputLayout
        val tietFechaSeguimiento = findView(R.id.tiet_venta_fecha_seguimiento) as TextInputEditText
        val tilDescripcion = findView(R.id.til_venta_descripcion) as TextInputLayout
        val tietDescripcion = findView(R.id.tiet_venta_descripcion) as TextInputEditText
        val btnCancelar = findView(R.id.btn_cancelar_venta) as MaterialButton
        val btnGuardar = findView(R.id.btn_guardar_venta) as MaterialButton

        val clienteMap = clientesDisponibles.associateBy { it.label }

        actvCliente.keyListener = null
        actvCliente.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, clientesDisponibles.map { it.label })
        )

        val now = System.currentTimeMillis()
        setupDateField(tietFechaVenta, now)
        setupDateField(tietFechaSeguimiento, now)

        if (clientesDisponibles.isEmpty()) {
            tilCliente.error = "No hay clientes disponibles"
            btnGuardar.isEnabled = false
        } else {
            btnGuardar.isEnabled = true
        }

        actvCliente.doAfterTextChanged { tilCliente.error = null }
        tietMontoTotal.doAfterTextChanged { tilMontoTotal.error = null }
        tietPagoTotal.doAfterTextChanged { tilPagoTotal.error = null }
        tietFechaVenta.doAfterTextChanged { tilFechaVenta.error = null }
        tietFechaSeguimiento.doAfterTextChanged { tilFechaSeguimiento.error = null }
        tietDescripcion.doAfterTextChanged { tilDescripcion.error = null }

        btnCancelar.setOnClickListener { onCancelar() }

        btnGuardar.setOnClickListener {
            val result = validarVentaFrontend(
                clienteSeleccionado = actvCliente.text?.toString()?.trim().orEmpty(),
                montoText = tietMontoTotal.text?.toString()?.trim().orEmpty(),
                pagoText = tietPagoTotal.text?.toString()?.trim().orEmpty(),
                fechaVenta = tietFechaVenta.tag as? Long,
                fechaSeguimiento = tietFechaSeguimiento.tag as? Long,
                descripcion = tietDescripcion.text?.toString()?.trim().orEmpty(),
                clienteMap = clienteMap,
                tilCliente = tilCliente,
                tilMontoTotal = tilMontoTotal,
                tilPagoTotal = tilPagoTotal,
                tilFechaVenta = tilFechaVenta,
                tilFechaSeguimiento = tilFechaSeguimiento,
                tilDescripcion = tilDescripcion
            )

            if (result != null) onGuardar(result)
        }
    }

    private fun setupDateField(field: TextInputEditText, initialMillis: Long) {
        field.tag = initialMillis
        field.setText(dateFormatter.format(initialMillis))
        field.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = field.tag as? Long ?: System.currentTimeMillis() }
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
            tilCliente.error = "Seleccione un cliente valido"
            esValido = false
        } else {
            tilCliente.error = null
        }

        val monto = montoText.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            tilMontoTotal.error = "Ingrese un monto total valido"
            esValido = false
        } else {
            tilMontoTotal.error = null
        }

        val pago = pagoText.toDoubleOrNull()
        if (pago == null || pago < 0.0) {
            tilPagoTotal.error = "Ingrese un pago total valido"
            esValido = false
        } else if (monto != null && pago > monto) {
            tilPagoTotal.error = "El pago total no puede superar el monto"
            esValido = false
        } else {
            tilPagoTotal.error = null
        }

        if (fechaVenta == null) {
            tilFechaVenta.error = "Seleccione la fecha de venta"
            esValido = false
        } else {
            tilFechaVenta.error = null
        }

        if (fechaSeguimiento == null) {
            tilFechaSeguimiento.error = "Seleccione la fecha de seguimiento"
            esValido = false
        } else {
            tilFechaSeguimiento.error = null
        }

        if (descripcion.isEmpty()) {
            tilDescripcion.error = "Ingrese una descripcion"
            esValido = false
        } else {
            tilDescripcion.error = null
        }

        if (!esValido || cliente == null || monto == null || pago == null || fechaVenta == null || fechaSeguimiento == null) {
            return null
        }

        return VentaFormData(
            clienteId = cliente.id,
            nombreCliente = cliente.nombre,
            montoTotal = monto,
            pagoTotal = pago,
            fechaVenta = fechaVenta,
            fechaSeguimiento = fechaSeguimiento,
            descripcion = descripcion
        )
    }

    private fun registrarVenta(form: VentaFormData, dialog: Dialog) {
        lifecycleScope.launch {

            val db = AppDatabase.getDatabase(requireContext())

            // OBTENER EL CLIENTE REAL DESDE LA BD
            val userMail = sessionManager.getUserMail()

            val cliente = db.clienteDao()
                .obtenerTodos(userMail)
                .find { it.id == form.clienteId }

            val estado = if (form.pagoTotal >= form.montoTotal) "Pagado" else "Pendiente"

            val venta = Venta(
                idClienteVenta = form.clienteId,
                nombreCliente = form.nombreCliente,
                userMail = userMail,

                // NUEVOS CAMPOS ( NECESARIOS PARA FILTRAR POR EMAIL Y TELÉFONO )
                emailCliente = cliente?.email ?: "",
                telefonoCliente = cliente?.telefono ?: "",

                montoTotal = form.montoTotal,
                pagoTotal = form.pagoTotal,
                fechaVenta = form.fechaVenta,
                fechaSeguimiento = form.fechaSeguimiento,
                descripcion = form.descripcion,
                total = form.montoTotal,
                fecha = form.fechaVenta,
                formaPago = "Pago total",
                estado = estado
            )

            db.ventaDao().insert(venta)

            actualizarEstadoCliente(form.clienteId)
            cargarVentas()

            Toast.makeText(requireContext(), "Venta guardada con éxito", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE
        fabOverlay.isClickable = true
        fabOverlay.isFocusable = true

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

    /* --------------------------------------------------
              ACTUALIZAR EL ESTADO DEL CLIENTE
    -------------------------------------------------- */

    private suspend fun actualizarEstadoCliente(clienteId: Int) {

        val db = AppDatabase.getDatabase(requireContext())

        val userMail = sessionManager.getUserMail()

        val estados = db.ventaDao().obtenerEstadosPorCliente(clienteId, userMail)

        val nuevoEstado = when {
            estados.isEmpty() -> "No Asignado"
            estados.any { it.lowercase() == "pendiente" } -> "Pendiente"
            else -> "Vendido"
        }

        val cliente = db.clienteDao()
            .obtenerTodos(userMail)
            .find { it.id == clienteId }

        if (cliente != null) {
            db.clienteDao().update(
                cliente.copy(estado = nuevoEstado)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }
}