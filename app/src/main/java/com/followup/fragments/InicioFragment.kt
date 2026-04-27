package com.followup.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.SeguimientoHomeAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Venta
import com.followup.presentation.settings.Configuracion
import com.followup.presentation.settings.SessionManager
import de.hdodenhof.circleimageview.CircleImageView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

class InicioFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sessionManager: SessionManager
    private lateinit var barChart: BarChart

    private lateinit var rvSeguimientos: RecyclerView
    private lateinit var rvVentasRecientes: RecyclerView
    private lateinit var seguimientosAdapter: SeguimientoHomeAdapter
    private lateinit var ventasAdapter: SeguimientoHomeAdapter

    private var listaCompletaSeguimientos: List<Venta> = emptyList()
    private var listaCompletaVentas: List<Venta> = emptyList()

    private var expandidoSeguimientos = false
    private var expandidoVentas = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        initServices()
        initComponents(view)
        setupRecyclerViews()
        initListeners(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun initServices() {
        sessionManager = SessionManager(requireContext())
        sharedPreferences = requireActivity().getSharedPreferences("FollowUp_prefs", Context.MODE_PRIVATE)
    }

    private fun initComponents(view: View) {
        barChart = view.findViewById(R.id.barChart)
        rvSeguimientos = view.findViewById(R.id.rv_seguimientos)
        rvVentasRecientes = view.findViewById(R.id.rv_ventas_recientes)
        configurarImagenPerfil(view)
        setupChartStyle()
    }
// Configura el estilo del gráfico de barras
    private fun setupChartStyle() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setTouchEnabled(false)
        
        val xAxis = barChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.textSize = 10f
        xAxis.granularity = 1f

        val axisLeft = barChart.axisLeft
        axisLeft.setDrawGridLines(false)
        axisLeft.setDrawAxisLine(false)
        axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        axisLeft.textSize = 10f
        axisLeft.axisMinimum = 0f

        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setNoDataText(getString(R.string.cargando_datos))
        barChart.extraBottomOffset = 10f
    }
// Carga los datos de ventas y actualiza la interfaz
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userMail = sessionManager.getUserMail()
                val ventas = db.ventaDao().obtenerTodas(userMail)

                actualizarMetricas(db, ventas, userMail)
                actualizarGrafico(ventas)
                procesarSeguimientos(ventas)
                procesarVentas(ventas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Actualiza el gráfico de barras con los datos de facturación por mes
    private fun actualizarGrafico(ventas: List<Venta>) {
        val ventasPagadas = ventas.filter { it.estado == "Pagado" }
        val cal = Calendar.getInstance()
        val meses = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val facturacionPorMes = FloatArray(12)

        for (venta in ventasPagadas) {
            cal.timeInMillis = venta.fechaVenta
            val mes = cal.get(Calendar.MONTH)
            facturacionPorMes[mes] += venta.total.toFloat()
        }

        val entries = mutableListOf<BarEntry>()
        for (i in 0..11) {
             entries.add(BarEntry(i.toFloat(), facturacionPorMes[i]))
        }

        val dataSet = BarDataSet(entries, getString(R.string.facturacion))
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        
        barChart.data = barData
        
        barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return meses.getOrNull(value.toInt()) ?: ""
            }
        }
        
        barChart.xAxis.labelCount = 12
        barChart.animateY(800)
        barChart.invalidate()
    }

    private fun configurarImagenPerfil(view: View) {
        val prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val uriString = prefs.getString("profile_image_uri", null)
    }

    private fun setupRecyclerViews() {
        seguimientosAdapter = SeguimientoHomeAdapter(false)
        rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
        rvSeguimientos.adapter = seguimientosAdapter
        ventasAdapter = SeguimientoHomeAdapter(true)
        rvVentasRecientes.layoutManager = LinearLayoutManager(requireContext())
        rvVentasRecientes.adapter = ventasAdapter
    }

    private fun initListeners(view: View) {
        setupNavegacionRapida(view)
        setupToggles(view)
    }

    private fun setupNavegacionRapida(view: View) {
        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        view.findViewById<View>(R.id.card_clientes)?.setOnClickListener { bottomNav.selectedItemId = R.id.bottom_Clientes }
        view.findViewById<View>(R.id.card_ventas)?.setOnClickListener { bottomNav.selectedItemId = R.id.bottom_Ventas }
    }

    private fun setupToggles(view: View) {
        val layoutSeg = view.findViewById<View>(R.id.layoutToggle_Seguimientos)
        val tvSeg = view.findViewById<TextView>(R.id.tvToggle_Seguimientos)
        val ivSeg = view.findViewById<ImageView>(R.id.ivToggle_Seguimientos)
        val layoutVen = view.findViewById<View>(R.id.layoutToggle_VentasRec)
        val tvVen = view.findViewById<TextView>(R.id.tvToggle_VentasRec)
        val ivVen = view.findViewById<ImageView>(R.id.ivToggle_VentasRec)

        layoutSeg.setOnClickListener {
            expandidoSeguimientos = !expandidoSeguimientos
            actualizarListaSeguimientos()
            animarToggle(ivSeg, tvSeg, expandidoSeguimientos)
        }
        layoutVen.setOnClickListener {
            expandidoVentas = !expandidoVentas
            actualizarListaVentas()
            animarToggle(ivVen, tvVen, expandidoVentas)
        }
    }

    private fun animarToggle(iv: ImageView, tv: TextView, expandido: Boolean) {
        iv.animate().rotation(if (expandido) 90f else 0f).setDuration(200).start()
        tv.text = if (expandido) getString(R.string.ver_menos) else getString(R.string.ver_todos)
    }

    fun actualizarSaludo() {
        val userName = sharedPreferences.getString("USER_NAME", "Usuario")
        view?.findViewById<TextView>(R.id.tv_saludo)?.text = getString(R.string.saludo_formato, userName)
    }

    // Actualiza las métricas de clientes, ventas y alertas en la interfaz
    private suspend fun actualizarMetricas(db: AppDatabase, ventas: List<Venta>, userMail: String) {
        val clientesCount = db.clienteDao().obtenerTodos(userMail).size
        val ventasCount = ventas.size
        val hoy = obtenerInicioDelDia()
        val alertasCount = ventas.count { it.estado == "Pendiente" && it.fechaSeguimiento >= it.fechaVenta && it.fechaSeguimiento >= hoy }
        view?.let {
            it.findViewById<TextView>(R.id.tv_count_clientes).text = clientesCount.toString()
            it.findViewById<TextView>(R.id.tv_count_ventas).text = ventasCount.toString()
            it.findViewById<TextView>(R.id.tv_count_alertas).text = alertasCount.toString()
        }
    }

    private fun procesarSeguimientos(ventas: List<Venta>) {
        val hoy = obtenerInicioDelDia()
        listaCompletaSeguimientos = ventas.filter { it.estado == "Pendiente" && it.fechaSeguimiento >= it.fechaVenta && it.fechaSeguimiento >= hoy }.sortedBy { it.fechaSeguimiento }
        actualizarListaSeguimientos()
    }

    private fun procesarVentas(ventas: List<Venta>) {
        listaCompletaVentas = ventas.filter { it.estado == "Pagado" }.sortedByDescending { it.fechaVenta }
        actualizarListaVentas()
    }

    private fun actualizarListaSeguimientos() {
        val lista = if (expandidoSeguimientos) listaCompletaSeguimientos.toList() else listaCompletaSeguimientos.take(3).toList()
        seguimientosAdapter.submitList(lista)
    }

    private fun actualizarListaVentas() {
        val lista = if (expandidoVentas) listaCompletaVentas.toList() else listaCompletaVentas.take(3).toList()
        ventasAdapter.submitList(lista)
    }

    private fun obtenerInicioDelDia(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
