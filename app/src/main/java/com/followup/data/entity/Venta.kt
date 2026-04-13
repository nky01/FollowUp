package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Ventas_Tabla")
data class Venta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val idClienteVenta: Int,
    val clienteNombre: String,
    val total: Double,
    val pagoTotal: Double,
    val fecha: String,
    val fechaSeguimiento: String,
    val estado: String
)