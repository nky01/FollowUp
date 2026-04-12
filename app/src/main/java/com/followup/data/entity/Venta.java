package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Ventas_Tabla")
data class Venta(
        @PrimaryKey(autoGenerate = true)
        val ID_Venta: Int = 0,
        val ID_Cliente_Venta: Int,
        val Total: Double,
        val Fecha: Long,
        val Forma_Pago: String,
        val Estado: String
)