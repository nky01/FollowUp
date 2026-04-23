package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Cliente_Tabla")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val userMail: String,

    val nombre: String,
    val descripcion: String,
    val telefono: String,
    val email: String,
    val estado: String, // "Pendiente", "Vendido", "No Asignado"
    val isDeleted: Boolean = false,

    val fecha: Long, // Tiempo en milisegundos

    var expandido: Boolean = false // ESTADO DE EXPANSIÓN PARA EL ITEM
)