package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/* ========================================================================================
                                    ESTADOS DEL CLIENTE
   ========================================================================================
   Los estados son automáticos — nunca los elige el usuario manualmente.

   NUEVO_CLIENTE   → Se asigna al crear. Dura 24hs. Luego pasa a NO_ASIGNADO
                     (o a PAGO_PENDIENTE si tiene ventas pendientes).
   PAGO_PENDIENTE  → El cliente tiene al menos una venta con estado "Pendiente".
   PAGO_REALIZADO  → Todas las ventas pasaron a "Pagado". Dura 24hs.
                     Luego: si queda alguna pendiente → PAGO_PENDIENTE, sino → NO_ASIGNADO.
   NO_ASIGNADO     → Estado neutro. Sin ventas activas pendientes.
*/
object EstadoCliente {
    const val NUEVO_CLIENTE  = "Nuevo Cliente"
    const val PAGO_PENDIENTE = "Pago Pendiente"
    const val PAGO_REALIZADO = "Pago Realizado"
    const val NO_ASIGNADO    = "No Asignado"

    const val DURACION_TRANSITORIO_MS = 24 * 60 * 60 * 1000L // 24 horas en milisegundos
}

@Entity(tableName = "Cliente_Tabla")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val userMail: String,

    val nombre: String,
    val apellido: String = "",
    val direccion: String = "",
    val descripcion: String,
    val telefono: String,
    val email: String,

    /**
     * Estado actual del cliente. Siempre uno de los valores en [EstadoCliente].
     * Se asigna automáticamente — nunca por input del usuario.
     */
    val estado: String = EstadoCliente.NUEVO_CLIENTE,

    val isDeleted: Boolean = false,

    /** Timestamp de creación (ms). Usado para calcular expiración del estado NUEVO_CLIENTE. */
    val fecha: Long = System.currentTimeMillis(),

    /**
     * Timestamp del último cambio de estado transitorio (NUEVO_CLIENTE o PAGO_REALIZADO).
     * Cuando este campo + 24hs < ahora, el estado debe recalcularse.
     * Null si el estado actual no es transitorio.
     */
    val fechaCambioEstado: Long? = null,
)