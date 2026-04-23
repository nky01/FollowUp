package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Ventas_Tabla")
data class Venta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val userMail: String,

    val idClienteVenta: Int,
    val nombreCliente: String,
    val montoTotal: Double,
    val pagoTotal: Double,
    val fechaVenta: Long,
    val fechaSeguimiento: Long,
    val descripcion: String,
    // Campos legacy mantenidos para no romper vistas/consultas existentes durante la transicion.
    val total: Double,
    val fecha: Long,
    val formaPago: String,
    val estado: String,
    val isDeleted: Boolean = false,

    // CAMPOS PARA EL FILTRADO DEL FRAGMENT VENTAS
    val emailCliente: String,
    val telefonoCliente: String
)