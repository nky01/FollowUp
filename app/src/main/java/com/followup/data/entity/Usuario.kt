package com.followup.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Usuario_Tabla")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val mail: String,
    val contraseniaHash: String,
    val codigo2FA: String?,
    val imagenPerfil: String? = null // Nueva columna para persistir la URI de la imagen
)