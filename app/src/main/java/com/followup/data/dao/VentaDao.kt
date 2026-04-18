package com.followup.data.dao

import androidx.room.*
import com.followup.data.entity.Venta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: Venta)

    @Update
    suspend fun update(venta: Venta)

    @Delete
    suspend fun delete(venta: Venta)

    @Query("SELECT * FROM Ventas_Tabla WHERE isDeleted = 0 ORDER BY fecha DESC")
    suspend fun obtenerTodas(): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE idClienteVenta = :clienteId AND isDeleted = 0")
    suspend fun obtenerVentasPorCliente(clienteId: Int): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE estado = :estado AND isDeleted = 0 ORDER BY fecha DESC")
    suspend fun obtenerPorEstado(estado: String): List<Venta>

    @Query("SELECT SUM(total) FROM Ventas_Tabla WHERE estado = 'Pagado' AND isDeleted = 0")
    suspend fun obtenerIngresosTotales(): Double?

    @Query("SELECT estado FROM Ventas_Tabla WHERE idClienteVenta = :clienteId AND isDeleted = 0")
    suspend fun obtenerEstadosPorCliente(clienteId: Int): List<String>

    @Query("SELECT * FROM Ventas_Tabla WHERE isDeleted = 0")
    fun getVentasActivas(): Flow<List<Venta>>

    @Query("SELECT * FROM Ventas_Tabla WHERE isDeleted = 1")
    fun getVentasEliminadas(): Flow<List<Venta>>

    @Query("UPDATE Ventas_Tabla SET isDeleted = 1 WHERE id = :idVenta")
    suspend fun softDelete(idVenta: Int)

    @Query("UPDATE Ventas_Tabla SET isDeleted = 0 WHERE id = :idVenta")
    suspend fun restaurarVenta(idVenta: Int)
}