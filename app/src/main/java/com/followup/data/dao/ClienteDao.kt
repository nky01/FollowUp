package com.followup.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.followup.data.entity.Cliente

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

    // Obtener Una lista de todos los clientes
        // Usado Para: Mostrar Clientes en el RecyclerView de ClientesFragment
    @Query("SELECT * FROM Cliente_Tabla")
    fun obtenerClientes(): LiveData<List<Cliente>> // LiveData --> para mantener los datos actualizados
}