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

    /* ------------------------------
            INSERTAR CLIENTE
    ------------------------------ */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: Cliente)

    /* ------------------------------
            ACTUALIZAR CLIENTE
    ------------------------------ */
    @Update
    suspend fun update(cliente: Cliente)

    /* ------------------------------
            ELIMINAR CLIENTE
    ------------------------------ */
    @Delete
    suspend fun delete(cliente: Cliente)

    /* ------------------------------
        OBTENER TODOS (POR USUARIO)
    ------------------------------ */
    @Query("""
        SELECT * FROM Cliente_Tabla
        WHERE userMail = :userMail
        AND isDeleted = 0
        ORDER BY fecha DESC
    """)
    suspend fun obtenerTodos(userMail: String): List<Cliente>

    /* ------------------------------
        OBTENER POR EMAIL (MISMO USER)
    ------------------------------ */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE email = :email 
        AND userMail = :userMail 
        LIMIT 1
    """)
    suspend fun obtenerPorEmail(email: String, userMail: String): Cliente?

    /* ------------------------------
        OBTENER POR ESTADO + USER
    ------------------------------ */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE estado = :estado 
        AND isDeleted = 0 
        AND userMail = :userMail
        ORDER BY fecha DESC
    """)
    suspend fun obtenerPorEstado(estado: String, userMail: String): List<Cliente>

    /* ------------------------------
        SOFT DELETE
    ------------------------------ */
    @Query("UPDATE Cliente_Tabla SET isDeleted = 1 WHERE id = :clienteId")
    suspend fun marcarComoEliminado(clienteId: Int)

    /* ------------------------------
        FLOW ACTIVOS (POR USER)
    ------------------------------ */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE isDeleted = 0 
        AND userMail = :userMail
    """)
    fun getClientesActivos(userMail: String): Flow<List<Cliente>>

    /* ------------------------------
        FLOW PAPELERA (POR USER)
    ------------------------------ */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE isDeleted = 1 
        AND userMail = :userMail
    """)
    fun getClientesEnPapelera(userMail: String): Flow<List<Cliente>>
}