package com.followup.ui

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.followup.R
import com.followup.data.entity.EstadoCliente
import com.google.android.material.card.MaterialCardView

/**
 * Helper centralizado para aplicar colores de estado.
 *
 * Todos los Fragments y Adapters deben usar estas funciones
 * en lugar de Color.parseColor("#XXXXXX") hardcodeado.
 *
 * Al usar @color/estado_* del colors.xml, el modo oscuro y el modo
 * daltónico funcionan automáticamente sin tocar este archivo.
 */
object EstadoColorHelper {

    /* --------------------------------------------------
            COLORES POR ESTADO — devuelve el resource id
    -------------------------------------------------- */

    /** Color del stroke/borde de la card según estado del cliente. */
    fun strokeColorRes(estado: String): Int = when (estado) {
        EstadoCliente.NUEVO_CLIENTE  -> R.color.estado_nuevo_stroke
        EstadoCliente.PAGO_PENDIENTE -> R.color.estado_pendiente_stroke
        EstadoCliente.PAGO_REALIZADO -> R.color.estado_realizado_stroke
        EstadoCliente.PAGO_CADUCADO  -> R.color.estado_caducado_stroke
        else                         -> R.color.estado_no_asignado_stroke
    }

    /** Color del fondo del badge de estado. */
    fun badgeBgColorRes(estado: String): Int = when (estado) {
        EstadoCliente.NUEVO_CLIENTE  -> R.color.estado_nuevo_stroke
        EstadoCliente.PAGO_PENDIENTE -> R.color.estado_pendiente_stroke
        EstadoCliente.PAGO_REALIZADO -> R.color.estado_realizado_stroke
        EstadoCliente.PAGO_CADUCADO  -> R.color.estado_caducado_stroke
        else                         -> R.color.estado_no_asignado_stroke
    }

    /** Color del badge para estados de venta (string libre). */
    fun badgeBgColorResVenta(estado: String): Int = when (estado.lowercase()) {
        "pagado"       -> R.color.estado_realizado_stroke
        "pendiente"    -> R.color.estado_pendiente_stroke
        "pago caducado"-> R.color.estado_caducado_stroke
        else           -> R.color.estado_no_asignado_stroke
    }

    /* --------------------------------------------------
            FUNCIONES DE APLICACIÓN DIRECTA
    -------------------------------------------------- */

    /**
     * Aplica el color de stroke a una MaterialCardView según el estado del cliente.
     * Reemplaza: card.strokeColor = Color.parseColor("#XXXXXX")
     */
    fun aplicarStrokeCard(context: Context, card: MaterialCardView, estado: String) {
        card.strokeColor = ContextCompat.getColor(context, strokeColorRes(estado))
    }

    /**
     * Aplica el color de fondo al badge de estado de un TextView.
     * Reemplaza: tvEstado.backgroundTintList = ColorStateList.valueOf(Color.parseColor(...))
     */
    fun aplicarBadgeCliente(context: Context, tvEstado: TextView, estado: String) {
        val color = ContextCompat.getColor(context, badgeBgColorRes(estado))
        tvEstado.backgroundTintList = ColorStateList.valueOf(color)
    }

    /**
     * Aplica el color de fondo al badge de estado de una venta.
     */
    fun aplicarBadgeVenta(context: Context, tvEstado: TextView, estado: String) {
        val color = ContextCompat.getColor(context, badgeBgColorResVenta(estado))
        tvEstado.backgroundTintList = ColorStateList.valueOf(color)
    }

    /**
     * Aplica stroke + badge juntos (para el item_cliente).
     */
    fun aplicarEstadoCompleto(context: Context, card: MaterialCardView, tvEstado: TextView, estado: String) {
        aplicarStrokeCard(context, card, estado)
        aplicarBadgeCliente(context, tvEstado, estado)
    }
}