package com.followup.fragments.menuLateral

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Venta
import com.followup.presentation.settings.SessionManager
import com.followup.ui.EstadoColorHelper
import com.google.android.material.button.MaterialButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

class AgendaFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var sessionManager: SessionManager

    private val titleFormatter = SimpleDateFormat("EEEE dd 'de' MMMM", Locale("es", "AR"))
    private val moneyFmt = DecimalFormat("#,##0.00")
    private var ventasPorDia: Map<LocalDate, List<Venta>> = emptyMap()

    /* ========================================================================================
                                        CICLO DE VIDA
    ======================================================================================== */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_agenda, container, false)
        
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
        calendarView = view.findViewById(R.id.calendarView)
        cargarEventos()
    }

    /* ========================================================================================
                                    CARGA DE DATOS
    ======================================================================================== */

    private fun cargarEventos() {
        lifecycleScope.launch {
            val userMail = sessionManager.getUserMail()
            val ventas = AppDatabase.getDatabase(requireContext())
                .ventaDao().obtenerTodas(userMail)
                .filter { it.estado == "Pendiente" || it.estado == "Pago caducado" }

            ventasPorDia = ventas.groupBy {
                java.time.Instant.ofEpochMilli(it.fechaSeguimiento)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            }

            configurarCalendario()
        }
    }

    /* ========================================================================================
                                    CONFIGURACIÓN DEL CALENDARIO
    ======================================================================================== */

    private fun configurarCalendario() {
        val currentMonth = YearMonth.now()
        calendarView.setup(
            currentMonth.minusMonths(6),
            currentMonth.plusMonths(12),
            firstDayOfWeekFromLocale()
        )
        calendarView.scrollToMonth(currentMonth)

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                val tvDay = container.containerView.findViewById<TextView>(R.id.tv_day)
                val dot   = container.containerView.findViewById<View>(R.id.dot_indicator)

                tvDay.text = data.date.dayOfMonth.toString()

                // Días fuera del mes actual → opacos y sin click
                if (data.position != DayPosition.MonthDate) {
                    tvDay.alpha = 0.3f
                    dot.visibility = View.INVISIBLE
                    container.containerView.setOnClickListener(null)
                    return
                }

                tvDay.alpha = 1f

                // Resaltar hoy
                if (data.date == LocalDate.now()) {
                    tvDay.setBackgroundResource(R.drawable.bg_avatar_circle)
                    tvDay.setTextColor(Color.parseColor("#286DFF"))
                } else {
                    tvDay.background = null
                    tvDay.setTextColor(Color.parseColor("#1D2939"))
                }

                // Punto indicador de ventas
                val ventasDelDia = ventasPorDia[data.date]
                if (ventasDelDia != null) {
                    dot.visibility = View.VISIBLE
                    val tieneCaducado = ventasDelDia.any { it.estado == "Pago caducado" }
                    dot.backgroundTintList = ColorStateList.valueOf(
                        Color.parseColor(if (tieneCaducado) "#F04438" else "#F79009")
                    )
                    container.containerView.setOnClickListener {
                        mostrarDialogDetalle(data.date, ventasDelDia)
                    }
                } else {
                    dot.visibility = View.INVISIBLE
                    container.containerView.setOnClickListener(null)
                }
            }
        }

        val mesFormatter = SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                val cal = Calendar.getInstance().apply {
                    set(data.yearMonth.year, data.yearMonth.monthValue - 1, 1)
                }
                container.tvMes.text = mesFormatter.format(cal.time)
                    .replaceFirstChar { it.uppercase() }
            }
        }
    }

    /* ========================================================================================
                                    DIALOG DETALLE DEL DÍA
    ======================================================================================== */

    private fun mostrarDialogDetalle(fecha: LocalDate, ventas: List<Venta>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agenda_detalle, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cal = Calendar.getInstance().apply {
            set(fecha.year, fecha.monthValue - 1, fecha.dayOfMonth)
        }
        dialogView.findViewById<TextView>(R.id.tv_agenda_fecha).text =
            titleFormatter.format(cal.time).replaceFirstChar { it.uppercase() }

        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_agenda_ventas)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = AgendaVentasAdapter(
            ventas     = ventas,
            onWspClick = { venta -> enviarWhatsApp(venta) },
            onMailClick= { venta -> enviarMail(venta) }
        )

        dialogView.findViewById<MaterialButton>(R.id.btn_cerrar_agenda)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /* ========================================================================================
                                    ACCIONES DE CONTACTO
    ======================================================================================== */

    /** Abre WhatsApp con un mensaje de recordatorio pre-armado. */
    private fun enviarWhatsApp(venta: Venta) {
        lifecycleScope.launch {
            val restante = venta.montoTotal - venta.pagoTotal
            val cliente  = AppDatabase.getDatabase(requireContext())
                .clienteDao().obtenerPorId(venta.idClienteVenta)

            val mensaje = """
                Hola ${venta.nombreCliente} 👋
                Te recuerdo que tenés un pago pendiente:
                💰 Monto restante: ${'$'}${moneyFmt.format(restante)}
                📋 Detalle: ${venta.descripcion}
                ¡Cualquier consulta avisame!
            """.trimIndent()

            val telefono = cliente?.telefono
                ?.replace(Regex("[^0-9]"), "")
                ?.let { if (it.startsWith("0")) it.substring(1) else it }
                ?.let { if (!it.startsWith("549")) "549$it" else it }

            val uri = if (!telefono.isNullOrEmpty()) {
                Uri.parse("https://wa.me/$telefono?text=${Uri.encode(mensaje)}")
            } else {
                Uri.parse("https://wa.me/?text=${Uri.encode(mensaje)}")
            }

            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    /** Abre el cliente de mail con asunto y cuerpo pre-armados. */
    private fun enviarMail(venta: Venta) {
        lifecycleScope.launch {
            val restante = venta.montoTotal - venta.pagoTotal
            val cliente  = AppDatabase.getDatabase(requireContext())
                .clienteDao().obtenerPorId(venta.idClienteVenta)

            val email = cliente?.email ?: ""

            val asunto = "Recordatorio de pago pendiente"
            val cuerpo = """
                Hola ${venta.nombreCliente},
                
                Te recordamos que tenés un pago pendiente en tu cuenta:
                
                💰 Monto restante: ${'$'}${moneyFmt.format(restante)}
                📋 Detalle: ${venta.descripcion}
                
                Por favor, regularizá tu situación a la brevedad.
                Ante cualquier consulta, no dudes en contactarnos.
                
                ¡Gracias!
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, cuerpo)
            }

            startActivity(Intent.createChooser(intent, "Enviar recordatorio por email"))
        }
    }

    /* ========================================================================================
                                    VIEW CONTAINERS
    ======================================================================================== */

    class DayViewContainer(view: View) : ViewContainer(view) {
        val containerView: View = view
    }

    class MonthViewContainer(view: View) : ViewContainer(view) {
        val tvMes: TextView = view.findViewById(R.id.tv_mes)
    }

    /* ========================================================================================
                                        ADAPTER
    ======================================================================================== */

    inner class AgendaVentasAdapter(
        private val ventas: List<Venta>,
        private val onWspClick: (Venta) -> Unit,
        private val onMailClick: (Venta) -> Unit
    ) : RecyclerView.Adapter<AgendaVentasAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCliente : TextView    = itemView.findViewById(R.id.tv_agenda_cliente)
            val tvEstado  : TextView    = itemView.findViewById(R.id.tv_agenda_estado)
            val tvMonto   : TextView    = itemView.findViewById(R.id.tv_agenda_monto_restante)
            val tvDesc    : TextView    = itemView.findViewById(R.id.tv_agenda_descripcion)
            val btnWsp    : LinearLayout = itemView.findViewById(R.id.btn_agenda_wsp)
            val btnMail   : LinearLayout = itemView.findViewById(R.id.btn_agenda_mail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_agenda_venta, parent, false)
        )

        override fun getItemCount() = ventas.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val venta    = ventas[position]
            val restante = venta.montoTotal - venta.pagoTotal

            holder.tvCliente.text = venta.nombreCliente
            holder.tvEstado.text  = venta.estado
            holder.tvMonto.text   = "Restante: ${'$'}${moneyFmt.format(restante)}"
            holder.tvDesc.text    = venta.descripcion.ifEmpty { "Sin descripción" }

            // Color dinámico del badge de estado
            EstadoColorHelper.aplicarBadgeVenta(holder.itemView.context, holder.tvEstado, venta.estado)

            holder.btnWsp.setOnClickListener  { onWspClick(venta) }
            holder.btnMail.setOnClickListener { onMailClick(venta) }
        }
    }
}