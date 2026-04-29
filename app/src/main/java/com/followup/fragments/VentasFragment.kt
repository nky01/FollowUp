package com.followup.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.VentasAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.EstadoCliente
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager
import com.followup.ui.EstadoColorHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import androidx.core.view.isVisible
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt

class VentasFragment : Fragment() {

    /* ========================================================================================
                                        COMPONENTES
    ======================================================================================== */

    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenuContainer: View
    private lateinit var fabOverlay: View
    private lateinit var btnNuevaVenta: MaterialButton
    private var isFabMenuOpen = false

    private lateinit var recyclerVentas: RecyclerView
    private lateinit var adapter: VentasAdapter
    private var listaOriginal: List<Venta> = listOf()

    private lateinit var btnFiltroEstado: MaterialCardView
    private lateinit var btnFilterDate: MaterialCardView
    private lateinit var dropdownEstadosVenta: LinearLayout

    private var filtroActual: String? = null
    private var rangoFechas: Pair<Long, Long>? = null

    private lateinit var inputBuscar: TextInputEditText
    private lateinit var sessionManager: SessionManager

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ventas, container, false)

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
        initComponents(view)
        initAdapter()
        initListeners()
        setupMenuButton(view)
        cargarVentas()
    }

    /* ========================================================================================
                                        INICIALIZACIÓN
    ======================================================================================== */

    private fun initComponents(view: View) {
        fabMain              = view.findViewById(R.id.fab_main)
        fabMenuContainer     = view.findViewById(R.id.fab_menu_container)
        fabOverlay           = view.findViewById(R.id.fab_overlay)
        btnNuevaVenta        = view.findViewById(R.id.btn_nueva_venta)
        recyclerVentas       = view.findViewById(R.id.rvVentas)
        btnFiltroEstado      = view.findViewById(R.id.btn_filtro_estado)
        btnFilterDate        = view.findViewById(R.id.btn_filter_date)
        dropdownEstadosVenta = view.findViewById(R.id.dropdown_estados_venta)
        inputBuscar          = view.findViewById(R.id.search)
    }

    private fun initAdapter() {
        adapter = VentasAdapter(object : VentasAdapter.OnVentaClickListener {
            override fun onDeleteClick(venta: Venta)  = mostrarDialogoEliminar(venta)
            override fun onEditClick(venta: Venta)    = mostrarDialogoEditar(venta)
            override fun onDetalleClick(venta: Venta) = mostrarDialogDetalle(venta)

            override fun onExportPdfClick(venta: Venta) {
                generarPdf(venta)
            }
        })
        recyclerVentas.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerVentas.clipChildren  = false
        recyclerVentas.clipToPadding = false
        recyclerVentas.adapter       = adapter
    }

    /* ========================================================================================
                                        LISTENERS
    ======================================================================================== */

    private fun initListeners() {
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevaVenta.setOnClickListener {
            closeFabMenu()
            showNuevaVentaDialog()
        }

        // Dropdown de estados
        btnFiltroEstado.setOnClickListener {
            dropdownEstadosVenta.visibility =
                if (dropdownEstadosVenta.isVisible) View.GONE else View.VISIBLE
        }

        requireView().findViewById<TextView>(R.id.filtro_venta_todos).setOnClickListener {
            filtroActual = null
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_pendiente).setOnClickListener {
            filtroActual = "Pendiente"
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_pagado).setOnClickListener {
            filtroActual = "Pagado"
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }
        requireView().findViewById<TextView>(R.id.filtro_venta_caducado).setOnClickListener {
            filtroActual = "Pago caducado"
            dropdownEstadosVenta.visibility = View.GONE
            filtrarVentas()
        }

        // Selector de rango de fechas
        btnFilterDate.setOnClickListener { mostrarSelectorFechas() }

        // Buscador
        inputBuscar.doAfterTextChanged { filtrarVentas() }
    }

    /* ========================================================================================
                                    CARGA Y FILTRADO
    ======================================================================================== */

    private fun cargarVentas() {
        lifecycleScope.launch {
            val userMail = sessionManager.getUserMail()
            listaOriginal = AppDatabase.getDatabase(requireContext())
                .ventaDao().obtenerTodas(userMail)
            filtrarVentas()
        }
    }

    /**
     * Aplica en orden: búsqueda por texto → filtro por estado → filtro por rango de fechas.
     * Ordena pendientes primero, luego por fecha descendente.
     */
    private fun filtrarVentas() {
        val query = inputBuscar.text.toString().lowercase()

        var lista = listaOriginal
            .sortedWith(compareBy<Venta> { it.estado.lowercase() != "pendiente" }
                .thenByDescending { it.fechaVenta })
            .filter {
                it.nombreCliente.lowercase().contains(query) ||
                        it.descripcion.lowercase().contains(query)
            }

        if (filtroActual != null) {
            lista = lista.filter { it.estado == filtroActual }
        }

        if (rangoFechas != null) {
            lista = lista.filter {
                it.fechaVenta >= rangoFechas!!.first!! &&
                        it.fechaVenta <= rangoFechas!!.second!!
            }
        }

        adapter.submitList(lista)
    }

    private fun mostrarSelectorFechas() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar periodo")
            .setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            rangoFechas = selection
            filtrarVentas()
            Toast.makeText(requireContext(), "Filtrando por fechas", Toast.LENGTH_SHORT).show()
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    /* ========================================================================================
                                    ACTUALIZAR ESTADO DEL CLIENTE
    ========================================================================================
    Se llama después de cada operación que cambia el estado de una venta
    (insertar, editar, eliminar) para mantener el estado del cliente sincronizado.
    */
    private suspend fun actualizarEstadoCliente(clienteId: Int, userMail: String) {
        val db  = AppDatabase.getDatabase(requireContext())
        val dao = db.clienteDao()
        val cliente = dao.obtenerPorId(clienteId) ?: return

        // Primero marcar ventas caducadas (seguimiento vencido) antes de contar
        // Caducado recién cuando termina el día de seguimiento, no durante el mismo día
        // Caducado solo si fechaSeguimiento es ANTERIOR a hoy (no el mismo día)
        val inicioDehoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        db.ventaDao().marcarVentasCaducadas(clienteId, userMail, inicioDehoy)

        val caducadas  = dao.contarVentasCaducadas(clienteId, userMail)
        val pendientes = dao.contarVentasPendientes(clienteId, userMail)
        val pagadas    = dao.contarVentasPagadas(clienteId, userMail)

        if (caducadas == 0 && pendientes == 0 && pagadas == 0) {
            dao.update(cliente.copy(
                estado            = EstadoCliente.NO_ASIGNADO,
                fechaCambioEstado = null
            ))
            return
        }

        val (nuevoEstado, fechaCambio) = when {
            caducadas  > 0 -> EstadoCliente.PAGO_CADUCADO  to null
            pendientes > 0 -> EstadoCliente.PAGO_PENDIENTE to null
            else -> EstadoCliente.PAGO_REALIZADO to System.currentTimeMillis()
        }

        if (cliente.estado == nuevoEstado && cliente.fechaCambioEstado == fechaCambio) return

        dao.update(cliente.copy(
            estado            = nuevoEstado,
            fechaCambioEstado = fechaCambio
        ))
    }

    /* ========================================================================================
                                    DIÁLOGO: NUEVA VENTA
    ======================================================================================== */

    private fun showNuevaVentaDialog() {
        lifecycleScope.launch {
            val db       = AppDatabase.getDatabase(requireContext())
            val userMail = sessionManager.getUserMail()
            val clientes = db.clienteDao().obtenerTodos(userMail).map {
                ClienteOption(it.id, it.nombre, "${it.nombre} - ${it.email}")
            }

            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_nueva_venta)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val actvCliente    = dialog.findViewById<AutoCompleteTextView>(R.id.actv_venta_cliente)
            val tietMonto      = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_monto_total)
            val tietPago       = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_pago_total)
            val tietFechaVenta = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_venta)
            val tietFechaSeg   = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_seguimiento)
            val tietDesc       = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_descripcion)
            val btnGuardar     = dialog.findViewById<MaterialButton>(R.id.btn_guardar_venta)
            val btnCancelar    = dialog.findViewById<MaterialButton>(R.id.btn_cancelar_venta)

            val clienteMap = clientes.associateBy { it.label }
            actvCliente.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, clientes.map { it.label })
            )

            setupDateField(tietFechaVenta, System.currentTimeMillis())
            setupDateField(tietFechaSeg, System.currentTimeMillis())

            btnCancelar.setOnClickListener { dialog.dismiss() }
            btnGuardar.setOnClickListener {
                val cliente = clienteMap[actvCliente.text.toString()]
                val monto   = tietMonto.text.toString().toDoubleOrNull()
                val pago    = tietPago.text.toString().toDoubleOrNull()

                if (cliente != null && monto != null && pago != null) {
                    registrarVenta(
                        VentaFormData(
                            clienteId        = cliente.id,
                            nombreCliente    = cliente.nombre,
                            montoTotal       = monto,
                            pagoTotal        = pago,
                            fechaVenta       = tietFechaVenta.tag as Long,
                            fechaSeguimiento = tietFechaSeg.tag as Long,
                            descripcion      = tietDesc.text.toString()
                        ), dialog
                    )
                } else {
                    Toast.makeText(requireContext(), "Completá los campos correctamente", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun registrarVenta(form: VentaFormData, dialog: Dialog) {
        lifecycleScope.launch {
            val userMail = sessionManager.getUserMail()
            val estado   = if (form.pagoTotal >= form.montoTotal) "Pagado" else "Pendiente"

            val venta = Venta(
                idClienteVenta   = form.clienteId,
                nombreCliente    = form.nombreCliente,
                montoTotal       = form.montoTotal,
                pagoTotal        = form.pagoTotal,
                fechaVenta       = form.fechaVenta,
                fechaSeguimiento = form.fechaSeguimiento,
                descripcion      = form.descripcion,
                total            = form.montoTotal,
                fecha            = form.fechaVenta,
                formaPago        = "Manual",
                estado           = estado,
                userMail         = userMail,
                emailCliente     = "",
                telefonoCliente  = ""
            )

            AppDatabase.getDatabase(requireContext()).ventaDao().insert(venta)
            actualizarEstadoCliente(form.clienteId, userMail) // ← sincroniza estado del cliente
            cargarVentas()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Venta registrada", Toast.LENGTH_SHORT).show()
        }
    }

    /* ========================================================================================
                                    DIÁLOGO: EDITAR VENTA
    ======================================================================================== */

    private fun mostrarDialogoEditar(venta: Venta) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_editar_venta)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tietMonto      = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_monto_total)
        val tietPago       = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_pago_total)
        val tietFechaVenta = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_venta)
        val tietFechaSeg   = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_fecha_seguimiento)
        val tietDesc       = dialog.findViewById<TextInputEditText>(R.id.tiet_venta_descripcion)
        val btnGuardar     = dialog.findViewById<MaterialButton>(R.id.btn_guardar_venta)
        val btnCancelar    = dialog.findViewById<MaterialButton>(R.id.btn_cancelar_venta)

        tietMonto.setText(venta.montoTotal.toString())
        tietPago.setText(venta.pagoTotal.toString())
        tietDesc.setText(venta.descripcion)
        setupDateField(tietFechaVenta, venta.fechaVenta)
        setupDateField(tietFechaSeg, venta.fechaSeguimiento)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val monto = tietMonto.text.toString().toDoubleOrNull()
            val pago  = tietPago.text.toString().toDoubleOrNull()

            if (monto != null && pago != null) {
                lifecycleScope.launch {
                    val userMail    = sessionManager.getUserMail()
                    val nuevoEstado = if (pago >= monto) "Pagado" else "Pendiente"

                    val ventaActualizada = venta.copy(
                        montoTotal       = monto,
                        pagoTotal        = pago,
                        fechaVenta       = tietFechaVenta.tag as Long,
                        fechaSeguimiento = tietFechaSeg.tag as Long,
                        descripcion      = tietDesc.text.toString(),
                        estado           = nuevoEstado,
                        total            = monto,
                        fecha            = tietFechaVenta.tag as Long
                    )

                    AppDatabase.getDatabase(requireContext()).ventaDao().update(ventaActualizada)
                    actualizarEstadoCliente(venta.idClienteVenta, userMail) // ← sincroniza estado del cliente
                    cargarVentas()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Venta actualizada", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /* ========================================================================================
                                    DIÁLOGO: ELIMINAR VENTA
    ======================================================================================== */

    private fun mostrarDialogoEliminar(venta: Venta) {
        val view   = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_eliminar_cliente,  null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        view.findViewById<TextView>(R.id.tv_titulo_eliminar).text =
            "¿Eliminar venta de ${venta.nombreCliente}?"
        view.findViewById<MaterialButton>(R.id.btn_eliminar_cliente).text = "Eliminar"

        view.findViewById<MaterialButton>(R.id.btn_cancelar_cliente).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_eliminar_cliente).setOnClickListener {
            lifecycleScope.launch {
                val userMail = sessionManager.getUserMail()
                AppDatabase.getDatabase(requireContext()).ventaDao().softDelete(venta.id)
                actualizarEstadoCliente(venta.idClienteVenta, userMail) // ← sincroniza estado del cliente
                cargarVentas()
                Toast.makeText(requireContext(), "Venta eliminada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /* ========================================================================================
                                    DIÁLOGO: DETALLE VENTA
    ======================================================================================== */

    private fun mostrarDialogDetalle(venta: Venta) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_detalle_venta)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Datos básicos
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_id).text         = "Venta #${venta.id}"
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_cliente).text     = venta.nombreCliente
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_estado).text      = venta.estado
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_monto).text       = "$${DecimalFormat("#,##0.00").format(venta.montoTotal)}"
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_pago).text        = "$${DecimalFormat("#,##0.00").format(venta.pagoTotal)}"
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_fecha).text       = dateFormatter.format(Date(venta.fechaVenta))
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_seguimiento).text = dateFormatter.format(Date(venta.fechaSeguimiento))
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_descripcion).text = venta.descripcion.ifEmpty { "Sin descripción" }

        // Progreso de pago
        val porcentaje = ((venta.pagoTotal / venta.montoTotal) * 100).toInt().coerceIn(0, 100)
        dialog.findViewById<TextView>(R.id.tv_detalle_venta_porcentaje).text  = "$porcentaje%"
        dialog.findViewById<ProgressBar>(R.id.progress_detalle_pago).progress = porcentaje

        // Badge de estado con color dinámico
        EstadoColorHelper.aplicarBadgeVenta(
            requireContext(),
            dialog.findViewById(R.id.tv_detalle_venta_estado),
            venta.estado
        )

        // Botón PDF — solo visible si está pagado
        val btnPdf = dialog.findViewById<View>(R.id.btn_detalle_venta_pdf)
        btnPdf.visibility = if (venta.estado == "Pagado") View.VISIBLE else View.GONE
        btnPdf.setOnClickListener {
            dialog.dismiss()
            generarPdf(venta)
        }

        // Botones de acción
        dialog.findViewById<View>(R.id.btn_detalle_venta_editar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEditar(venta)
        }
        dialog.findViewById<View>(R.id.btn_detalle_venta_eliminar).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEliminar(venta)
        }
        dialog.findViewById<View>(R.id.btn_cerrar_detalle_venta).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /* ========================================================================================
                                    SELECTOR DE FECHA
    ======================================================================================== */

    private fun setupDateField(field: TextInputEditText, initialMillis: Long) {
        field.tag = initialMillis
        field.setText(dateFormatter.format(Date(initialMillis)))
        field.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = field.tag as Long }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val selected = Calendar.getInstance().apply { set(year, month, day) }.timeInMillis
                field.tag = selected
                field.setText(dateFormatter.format(Date(selected)))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    /* ========================================================================================
                                        MENÚ FAB
    ======================================================================================== */

    private fun toggleFabMenu() { if (isFabMenuOpen) closeFabMenu() else openFabMenu() }

    private fun openFabMenu() {
        isFabMenuOpen = true
        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE
        fabOverlay.animate().alpha(1f).setDuration(200).start()
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(200).start()
        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false
        fabOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            fabOverlay.visibility = View.GONE
        }.start()
        fabMenuContainer.animate().alpha(0f).translationY(20f).setDuration(200).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()
        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }

    private fun generarPdf(venta: Venta) {
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(600, 900, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val softLine = Paint().apply {
            color = "#E5E7EB".toColorInt()
            strokeWidth = 1f
        }

        var y = 60

        val logo = BitmapFactory.decodeResource(resources, R.mipmap.logo_and_name)
        val scaledLogo = logo.scale(200, 60, false)
        canvas.drawBitmap(scaledLogo, 200f, 20f, null)

        y += 80

        val estadoColor = if (venta.pagoTotal >= venta.montoTotal) "#12B76A" else "#F79009"

        paint.color = estadoColor.toColorInt()
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(venta.estado.uppercase(), 300f, y.toFloat(), paint)

        y += 50

        paint.color = Color.BLACK
        paint.textSize = 36f
        paint.isFakeBoldText = true

        canvas.drawText("$${venta.montoTotal}", 300f, y.toFloat(), paint)

        y += 20

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Total pagado", 300f, y.toFloat(), paint)

        y += 60

        canvas.drawLine(80f, y.toFloat(), 520f, y.toFloat(), softLine)

        y += 40

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f

        canvas.drawText("Cliente", 80f, y.toFloat(), paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(venta.nombreCliente, 520f, y.toFloat(), paint)

        y += 30

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Fecha", 80f, y.toFloat(), paint)

        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(venta.fechaVenta))

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(fecha, 520f, y.toFloat(), paint)

        y += 30

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("ID operación", 80f, y.toFloat(), paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("#${venta.id}", 520f, y.toFloat(), paint)

        y += 40

        // Línea suave
        canvas.drawLine(80f, y.toFloat(), 520f, y.toFloat(), softLine)

        y += 40

        // 💸 DESGLOSE
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Monto total", 80f, y.toFloat(), paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${venta.montoTotal}", 520f, y.toFloat(), paint)

        y += 30

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Pagado", 80f, y.toFloat(), paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${venta.pagoTotal}", 520f, y.toFloat(), paint)

        y += 80

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        canvas.drawText("Comprobante generado por FollowUp", 300f, y.toFloat(), paint)

        pdfDocument.finishPage(page)

        try {
            val resolver = requireContext().contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "comprobante_${venta.id}.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                pdfDocument.writeTo(outputStream!!)
                outputStream.close()

                Toast.makeText(requireContext(), "Comprobante listo!", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }

    private fun setupMenuButton(view: View) {
        val btnMenu = view.findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener {
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.main)
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }
}