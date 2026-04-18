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

    @Query("SELECT * FROM Ventas_Tabla ORDER BY fecha DESC")
    suspend fun obtenerTodas(): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE idClienteVenta = :clienteId")
    suspend fun obtenerVentasPorCliente(clienteId: Int): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE estado = :estado ORDER BY fecha DESC")
    suspend fun obtenerPorEstado(estado: String): List<Venta>

    @Query("SELECT SUM(total) FROM Ventas_Tabla WHERE estado = 'Pagado'")
    suspend fun obtenerIngresosTotales(): Double?

    // QUERY PARA OBTENER LOS ESTADOS POR CLIENTE Y NO POR VENTA
    // [1] - SE UTILIZA PARA ACTUALIZAR EL ESTADO DEL CLIENTE CUANDO SE LE ASIGNA UNA VENTA
    @Query("SELECT estado FROM Ventas_Tabla WHERE idClienteVenta = :clienteId")
    suspend fun obtenerEstadosPorCliente(clienteId: Int): List<String>

    @Query("SELECT * FROM Ventas_Tabla WHERE isDeleted = 0")
    suspend fun getVentasActivas(): List<Venta>

    @Query("SELECT * FROM Ventas_Tabla WHERE isDeleted = 1")
    suspend fun getVentasEliminadas(): List<Venta>

    // El "Borrado Lógico"
    @Query("UPDATE Ventas_Tabla SET isDeleted = 1 WHERE id = :idVenta")
    suspend fun softDelete(idVenta: Int)

    // Para restaurar
    @Query("UPDATE Ventas_Tabla SET isDeleted = 0 WHERE id = :idVenta")
    suspend fun restaurarVenta(idVenta: Int)
}