package com.followup.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.followup.data.entity.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    // Insertar
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Si existe ID, reemplaza registro
    suspend fun insert(cliente: Cliente)

    // Actualizar
    @Update
    suspend fun update(cliente: Cliente)

    // Eliminar
    @Delete
    suspend fun delete(cliente: Cliente)

    /* Agregar Abajo los métodos de Filtrado */

}