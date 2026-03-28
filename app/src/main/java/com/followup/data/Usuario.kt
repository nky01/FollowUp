package com.followup.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Usuario")
data class Usuario(@PrimaryKey(autoGenerate = true)
                    val id:Int=0,
                   val nombre: String,
                   val mail: String,
                   val contraseniaHash: String,
                   val codigo2FA: String? // ?-puede ser null
)