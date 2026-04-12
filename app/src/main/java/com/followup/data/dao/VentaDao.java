package com.followup.data.dao

import androidx.room.*
import com.followup.data.entity.Venta

@Dao
interface VentaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: Venta)

    @Update
    suspend fun update(venta: Venta)

    @Delete
    suspend fun delete(venta: Venta)

    @Query("SELECT * FROM Ventas_Tabla ORDER BY Fecha DESC")
    suspend fun obtenerTodas(): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE ID_Cliente_Venta = :clienteId")
    suspend fun obtenerVentasPorCliente(clienteId: Int): List<Venta>

    @Query("SELECT SUM(Total) FROM Ventas_Tabla WHERE Estado = 'Pagado'")
    suspend fun obtenerIngresosTotales(): Double
}