package com.followup.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

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
        initComponents(view)
        initListeners()

        adapter = VentasAdapter(object : VentasAdapter.OnVentaClickListener {

            override fun onDeleteClick(venta: Venta) {
                mostrarDialogoEliminar(venta)
            }

            override fun onEditClick(venta: Venta) {
                Toast.makeText(requireContext(), "Editar venta", Toast.LENGTH_SHORT).show()
            }

        })

        recyclerVentas.layoutManager = LinearLayoutManager(requireContext())
        recyclerVentas.adapter = adapter

        cargarVentas()

    }

    private fun cargarVentas() {
        lifecycleScope.launch {
            val ventas = AppDatabase.getDatabase(requireContext())
                .ventaDao()
                .obtenerTodas()

            adapter.submitList(ventas)
        }
    }

    private fun mostrarDialogoEliminar(venta: Venta) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar venta")
            .setMessage("¿Estás seguro que querés eliminar esta venta?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext())
                        .ventaDao()
                        .delete(venta)

                    cargarVentas()
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
    }

    private fun initListeners() {
        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevaVenta.setOnClickListener {
            closeFabMenu()
            showNuevaVentaDialog()
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
        val clientes = AppDatabase.getDatabase(requireContext()).clienteDao().obtenerTodos()
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
            val estado = if (form.pagoTotal >= form.montoTotal) "Pagado" else "Pendiente"
            val venta = Venta(
                idClienteVenta = form.clienteId,
                nombreCliente = form.nombreCliente,
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

            AppDatabase.getDatabase(requireContext()).ventaDao().insert(venta)
            cargarVentas()
            Toast.makeText(requireContext(), "Venta guardada con exito", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        cargarVentas()
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

    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }
}