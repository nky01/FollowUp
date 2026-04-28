package com.followup.fragments.menuLateral

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.EstadoCliente
import com.followup.presentation.settings.SessionManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

class EstadisticasFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalVentas: TextView
    private lateinit var tvTasaCobro: TextView
    private lateinit var barChartIngresos: BarChart
    private lateinit var pieChartVentas: PieChart
    private lateinit var pieChartClientes: PieChart

    private lateinit var tvClienteMasPagadas: TextView
    private lateinit var tvClienteMasPagadasCount: TextView

    private lateinit var tvClienteMasPendientes: TextView
    private lateinit var tvClienteMasPendientesCount: TextView

    private lateinit var tvClienteMasCaducadas: TextView
    private lateinit var tvClienteMasCaducadasCount: TextView

    private val moneyFormatter = DecimalFormat("#,##0")
    private val meses = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_estadisticas, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, 0)
            insets
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        initComponents(view)
        setupCharts()
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    /* ========================================================================================
                                    INICIALIZACIÓN
    ======================================================================================== */

    private fun initComponents(view: View) {
        tvTotalIngresos  = view.findViewById(R.id.tv_total_ingresos)
        tvTotalVentas    = view.findViewById(R.id.tv_total_ventas)
        tvTasaCobro      = view.findViewById(R.id.tv_tasa_cobro)
        barChartIngresos = view.findViewById(R.id.barChartIngresos)
        pieChartVentas   = view.findViewById(R.id.pieChartVentas)
        pieChartClientes = view.findViewById(R.id.pieChartClientes)
        tvClienteMasPagadas = view.findViewById(R.id.tv_cliente_mas_pagadas)
        tvClienteMasPagadasCount = view.findViewById(R.id.tv_cliente_mas_pagadas_count)

        tvClienteMasPendientes = view.findViewById(R.id.tv_cliente_mas_pendientes)
        tvClienteMasPendientesCount = view.findViewById(R.id.tv_cliente_mas_pendientes_count)

        tvClienteMasCaducadas = view.findViewById(R.id.tv_cliente_mas_caducadas)
        tvClienteMasCaducadasCount = view.findViewById(R.id.tv_cliente_mas_caducadas_count)

        // Botón volver atrás
        view.findViewById<ImageButton>(R.id.btn_volver_atras).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /* ========================================================================================
                                    SETUP GRÁFICOS
    ======================================================================================== */

    private fun setupCharts() {
        setupBarChart()
        setupPieChart(pieChartVentas)
        setupPieChart(pieChartClientes)
    }

    private fun setupBarChart() {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        barChartIngresos.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(false)
            legend.isEnabled = false
            setNoDataText("Sin datos aún")

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                position = XAxis.XAxisPosition.BOTTOM
                this.textColor = textColor
                textSize = 9f
                granularity = 1f
                labelCount = 12
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) =
                        meses.getOrNull(value.toInt()) ?: ""
                }
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                this.textColor = textColor
                textSize = 9f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            extraBottomOffset = 8f
        }
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setTouchEnabled(false)
            setNoDataText("Sin datos aún")
        }
    }

    /* ========================================================================================
                                    CARGA DE DATOS
    ======================================================================================== */

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val db       = AppDatabase.getDatabase(requireContext())
                val userMail = sessionManager.getUserMail()
                val ventas   = db.ventaDao().obtenerTodas(userMail)
                val clientes = db.clienteDao().obtenerTodos(userMail)

                // ── Tarjetas resumen ──
                val totalIngresos = ventas
                    .filter { it.estado.equals("Pagado", ignoreCase = true) }
                    .sumOf { it.pagoTotal }

                val totalVentas = ventas.size

                val totalMonto = ventas.sumOf { it.montoTotal }
                val totalPagado = ventas.sumOf { it.pagoTotal }
                val tasaCobro = if (totalMonto > 0)
                    ((totalPagado / totalMonto) * 100).toInt() else 0

                tvTotalIngresos.text = "$${moneyFormatter.format(totalIngresos)}"
                tvTotalVentas.text   = "$totalVentas"
                tvTasaCobro.text     = "$tasaCobro%"

                // ── Bar chart: ingresos por mes ──
                val facturacionPorMes = FloatArray(12)
                val cal = Calendar.getInstance()
                ventas.filter { it.estado.equals("Pagado", ignoreCase = true) }.forEach { venta ->
                    cal.timeInMillis = venta.fechaVenta
                    facturacionPorMes[cal.get(Calendar.MONTH)] += venta.pagoTotal.toFloat()
                }

                val barEntries = (0..11).map { BarEntry(it.toFloat(), facturacionPorMes[it]) }
                val barDataSet = BarDataSet(barEntries, "Ingresos").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
                    setDrawValues(false)
                }
                barChartIngresos.data = BarData(barDataSet).apply { barWidth = 0.6f }
                barChartIngresos.animateY(800)
                barChartIngresos.invalidate()

                // ── Pie chart: ventas por estado ──
                val pagadas    = ventas.count { it.estado.equals("Pagado", ignoreCase = true) }.toFloat()
                val pendientes = ventas.count { it.estado.equals("Pendiente", ignoreCase = true) }.toFloat()
                val caducadas  = ventas.count { it.estado.equals("Pago caducado", ignoreCase = true) }.toFloat()

                if (pagadas + pendientes + caducadas > 0) {
                    val pieVentasEntries = mutableListOf<PieEntry>().apply {
                        if (pagadas > 0)    add(PieEntry(pagadas,    "Pagado"))
                        if (pendientes > 0) add(PieEntry(pendientes, "Pendiente"))
                        if (caducadas > 0)  add(PieEntry(caducadas,  "Caducado"))
                    }
                    val coloresVentas = mutableListOf<Int>().apply {
                        if (pagadas > 0)    add(ContextCompat.getColor(requireContext(), R.color.estado_realizado_stroke))
                        if (pendientes > 0) add(ContextCompat.getColor(requireContext(), R.color.estado_pendiente_stroke))
                        if (caducadas > 0)  add(ContextCompat.getColor(requireContext(), R.color.estado_caducado_stroke))
                    }
                    val pieVentasDataSet = PieDataSet(pieVentasEntries, "").apply {
                        colors = coloresVentas
                        sliceSpace = 3f
                        setDrawValues(false)
                    }
                    pieChartVentas.data = PieData(pieVentasDataSet)
                    pieChartVentas.animateY(800, Easing.EaseInOutQuad)
                    pieChartVentas.invalidate()
                }

                // ── Pie chart: clientes por estado ──
                val cNuevo     = clientes.count { it.estado == EstadoCliente.NUEVO_CLIENTE }.toFloat()
                val cPendiente = clientes.count { it.estado == EstadoCliente.PAGO_PENDIENTE }.toFloat()
                val cRealizado = clientes.count { it.estado == EstadoCliente.PAGO_REALIZADO }.toFloat()
                val cNoAsig    = clientes.count { it.estado == EstadoCliente.NO_ASIGNADO }.toFloat()
                val cCaducado  = clientes.count { it.estado == EstadoCliente.PAGO_CADUCADO }.toFloat()

                if (cNuevo + cPendiente + cRealizado + cNoAsig + cCaducado > 0) {
                    val pieClientesEntries = mutableListOf<PieEntry>().apply {
                        if (cNuevo > 0)     add(PieEntry(cNuevo,     "Nuevo"))
                        if (cPendiente > 0) add(PieEntry(cPendiente, "Pendiente"))
                        if (cRealizado > 0) add(PieEntry(cRealizado, "Realizado"))
                        if (cNoAsig > 0)    add(PieEntry(cNoAsig,    "No asignado"))
                        if (cCaducado > 0)  add(PieEntry(cCaducado,  "Caducado"))
                    }
                    val coloresClientes = mutableListOf<Int>().apply {
                        if (cNuevo > 0)     add(ContextCompat.getColor(requireContext(), R.color.estado_nuevo_stroke))
                        if (cPendiente > 0) add(ContextCompat.getColor(requireContext(), R.color.estado_pendiente_stroke))
                        if (cRealizado > 0) add(ContextCompat.getColor(requireContext(), R.color.estado_realizado_stroke))
                        if (cNoAsig > 0)    add(ContextCompat.getColor(requireContext(), R.color.estado_no_asignado_stroke))
                        if (cCaducado > 0)  add(ContextCompat.getColor(requireContext(), R.color.estado_caducado_stroke))
                    }
                    val pieClientesDataSet = PieDataSet(pieClientesEntries, "").apply {
                        colors = coloresClientes
                        sliceSpace = 3f
                        setDrawValues(false)
                    }
                    pieChartClientes.data = PieData(pieClientesDataSet)
                    pieChartClientes.animateY(800, Easing.EaseInOutQuad)
                    pieChartClientes.invalidate()
                }

                // ── ClientesTop ──
                fun clienteTopPorEstado(estado: String): Map.Entry<String, Int>? {
                    return ventas
                        .filter { it.estado.equals(estado, ignoreCase = true) }
                        .groupingBy { it.nombreCliente }
                        .eachCount()
                        .maxByOrNull { it.value }
                }

                val topPagadas = clienteTopPorEstado("Pagado")
                tvClienteMasPagadas.text = topPagadas?.key ?: "—"
                tvClienteMasPagadasCount.text = topPagadas?.value?.toString() ?: "0"

                val topPendientes = clienteTopPorEstado("Pendiente")
                tvClienteMasPendientes.text = topPendientes?.key ?: "—"
                tvClienteMasPendientesCount.text = topPendientes?.value?.toString() ?: "0"

                val topCaducadas = clienteTopPorEstado("Pago caducado")
                tvClienteMasCaducadas.text = topCaducadas?.key ?: "—"
                tvClienteMasCaducadasCount.text = topCaducadas?.value?.toString() ?: "0"

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}