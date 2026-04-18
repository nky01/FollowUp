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

    // Obtener todos los clientes, ordenados por fecha
    @Query("SELECT * FROM Cliente_Tabla ORDER BY fecha DESC") // Falta implementar
    suspend fun obtenerTodos(): List<Cliente>

    // Obtener un cliente por su email
    @Query("SELECT * FROM Cliente_Tabla WHERE email = :email LIMIT 1") // Se tendria que cambiar en un futuro?
    suspend fun obtenerPorEmail(email: String): Cliente?

    //clasificacio por estado: vendido, pendiente o no asignado
    @Query("SELECT * FROM Cliente_Tabla WHERE estado = :estado ORDER BY fecha DESC")
    suspend fun obtenerPorEstado(estado: String): List<Cliente>

    @Query("UPDATE Cliente_Tabla SET isDeleted = 1 WHERE id = :clienteId")
    suspend fun marcarComoEliminado(clienteId: Int)

    @Query("SELECT * FROM Cliente_Tabla WHERE isDeleted = 0")
    fun getClientesActivos(): Flow<List<Cliente>>

    @Query("SELECT * FROM Cliente_Tabla WHERE isDeleted = 1")
    fun getClientesEnPapelera(): Flow<List<Cliente>>
}