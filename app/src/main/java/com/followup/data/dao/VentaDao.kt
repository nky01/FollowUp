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

    /* --------------------------------------------------
                    OBTENER TODAS
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Ventas_Tabla 
        WHERE userMail = :userMail 
        AND isDeleted = 0 
        ORDER BY fecha DESC
    """)
    suspend fun obtenerTodas(userMail: String): List<Venta>

    /* --------------------------------------------------
                FILTRAR POR ESTADO
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Ventas_Tabla 
        WHERE estado = :estado 
        AND userMail = :userMail 
        AND isDeleted = 0 
        ORDER BY fecha DESC
    """)
    suspend fun obtenerPorEstado(estado: String, userMail: String): List<Venta>

    /* --------------------------------------------------
            VENTAS POR CLIENTE
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Ventas_Tabla 
        WHERE idClienteVenta = :clienteId 
        AND userMail = :userMail 
        AND isDeleted = 0
    """)
    suspend fun obtenerVentasPorCliente(clienteId: Int, userMail: String): List<Venta>

    /* --------------------------------------------------
            INGRESOS TOTALES
    -------------------------------------------------- */
    @Query("""
        SELECT SUM(total) FROM Ventas_Tabla 
        WHERE estado = 'Pagado' 
        AND userMail = :userMail 
        AND isDeleted = 0
    """)
    suspend fun obtenerIngresosTotales(userMail: String): Double?

    /* --------------------------------------------------
        ESTADOS DE UN CLIENTE
    -------------------------------------------------- */
    @Query("""
        SELECT estado FROM Ventas_Tabla 
        WHERE idClienteVenta = :clienteId 
        AND userMail = :userMail 
        AND isDeleted = 0
    """)
    suspend fun obtenerEstadosPorCliente(clienteId: Int, userMail: String): List<String>

    /* --------------------------------------------------
                FLOW ACTIVAS
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Ventas_Tabla 
        WHERE userMail = :userMail 
        AND isDeleted = 0
    """)
    fun getVentasActivas(userMail: String): Flow<List<Venta>>

    /* --------------------------------------------------
                FLOW ELIMINADAS
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Ventas_Tabla 
        WHERE userMail = :userMail 
        AND isDeleted = 1
    """)
    fun getVentasEliminadas(userMail: String): Flow<List<Venta>>

    /* --------------------------------------------------
                SOFT DELETE
    -------------------------------------------------- */
    @Query("""
        UPDATE Ventas_Tabla 
        SET isDeleted = 1 
        WHERE id = :idVenta
    """)
    suspend fun softDelete(idVenta: Int)

    @Query("""
        UPDATE Ventas_Tabla 
        SET isDeleted = 0 
        WHERE id = :idVenta
    """)
    suspend fun restaurarVenta(idVenta: Int)

    /** Elimina la venta de la base de datos de forma permanente. */
    @Query("DELETE FROM Ventas_Tabla WHERE id = :ventaId")
    suspend fun eliminarFisico(ventaId: Int)

    /**
     * Elimina físicamente TODAS las ventas de un cliente (activas y en papelera).
     * Se usa al hacer baja física de un cliente para limpiar en cascada.
     */
    @Query("DELETE FROM Ventas_Tabla WHERE idClienteVenta = :clienteId AND userMail = :userMail")
    suspend fun eliminarVentasPorCliente(clienteId: Int, userMail: String)
}