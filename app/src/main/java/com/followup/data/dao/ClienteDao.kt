package com.followup.data.dao

import androidx.room.*
import com.followup.data.entity.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    /* --------------------------------------------------
                        INSERTAR
    -------------------------------------------------- */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: Cliente)

    /* --------------------------------------------------
                        ACTUALIZAR
    -------------------------------------------------- */
    @Update
    suspend fun update(cliente: Cliente)

    /* --------------------------------------------------
                        SOFT DELETE
    -------------------------------------------------- */
    @Query("UPDATE Cliente_Tabla SET isDeleted = 1 WHERE id = :clienteId")
    suspend fun marcarComoEliminado(clienteId: Int)

    /* --------------------------------------------------
                OBTENER TODOS (activos, por usuario)
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Cliente_Tabla
        WHERE userMail = :userMail
        AND isDeleted = 0
        ORDER BY fecha DESC
    """)
    suspend fun obtenerTodos(userMail: String): List<Cliente>

    /* --------------------------------------------------
                OBTENER POR ID
    -------------------------------------------------- */
    @Query("SELECT * FROM Cliente_Tabla WHERE id = :clienteId LIMIT 1")
    suspend fun obtenerPorId(clienteId: Int): Cliente?

    /* --------------------------------------------------
                OBTENER POR EMAIL (mismo usuario)
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE email = :email 
        AND userMail = :userMail 
        LIMIT 1
    """)
    suspend fun obtenerPorEmail(email: String, userMail: String): Cliente?

    /* --------------------------------------------------
                OBTENER POR ESTADO
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE estado = :estado 
        AND isDeleted = 0 
        AND userMail = :userMail
        ORDER BY fecha DESC
    """)
    suspend fun obtenerPorEstado(estado: String, userMail: String): List<Cliente>

    /* --------------------------------------------------
        CLIENTES CON ESTADO TRANSITORIO VENCIDO
        Devuelve clientes cuyo estado es NUEVO_CLIENTE o PAGO_REALIZADO
        y ya pasaron las 24hs desde el cambio de estado.
        El Fragment/ViewModel los recalcula al cargar.
    -------------------------------------------------- */
    @Query("""
        SELECT * FROM Cliente_Tabla
        WHERE isDeleted = 0
        AND userMail = :userMail
        AND estado IN ('Nuevo Cliente', 'Pago Realizado')
        AND fechaCambioEstado IS NOT NULL
        AND fechaCambioEstado < :limiteMs
    """)
    suspend fun obtenerClientesConEstadoVencido(userMail: String, limiteMs: Long): List<Cliente>

    /* --------------------------------------------------
        CANTIDAD DE VENTAS PAGADAS POR CLIENTE
    -------------------------------------------------- */
    @Query("""
        SELECT COUNT(*) FROM Ventas_Tabla
        WHERE idClienteVenta = :clienteId
        AND userMail = :userMail
        AND isDeleted = 0
        AND estado = 'Pagado'
    """)
    suspend fun contarVentasPagadas(clienteId: Int, userMail: String): Int

    /* --------------------------------------------------
        CANTIDAD DE VENTAS PENDIENTES POR CLIENTE
    -------------------------------------------------- */
    @Query("""
        SELECT COUNT(*) FROM Ventas_Tabla
        WHERE idClienteVenta = :clienteId
        AND userMail = :userMail
        AND isDeleted = 0
        AND estado = 'Pendiente'
    """)
    suspend fun contarVentasPendientes(clienteId: Int, userMail: String): Int

    /* --------------------------------------------------
            obtenerClientesConSeguimientoVencido
    -------------------------------------------------- */

    @Query("""
    SELECT c.* FROM Cliente_Tabla c
    INNER JOIN Ventas_Tabla v ON v.idClienteVenta = c.id
    WHERE c.isDeleted = 0
    AND c.userMail = :userMail
    AND c.estado = 'Pago Pendiente'
    AND v.isDeleted = 0
    AND v.estado = 'Pendiente'
    AND v.fechaSeguimiento < :ahora
    GROUP BY c.id
""")
    suspend fun obtenerClientesConSeguimientoVencido(userMail: String, ahora: Long): List<Cliente>

    /* --------------------------------------------------
                    Contar Ventas Caducadas
    -------------------------------------------------- */

    /** Cuenta ventas con estado 'Pago caducado' activas para un cliente. */
    @Query("""
    SELECT COUNT(*) FROM Ventas_Tabla
    WHERE idClienteVenta = :clienteId
    AND userMail = :userMail
    AND isDeleted = 0
    AND estado = 'Pago caducado'
""")
    suspend fun contarVentasCaducadas(clienteId: Int, userMail: String): Int

    /* --------------------------------------------------
                FLOWS (observación reactiva)
    -------------------------------------------------- */

    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE isDeleted = 0 
        AND userMail = :userMail
        ORDER BY fecha DESC
    """)
    fun getClientesActivos(userMail: String): Flow<List<Cliente>>

    @Query("""
        SELECT * FROM Cliente_Tabla 
        WHERE isDeleted = 1 
        AND userMail = :userMail
    """)
    fun getClientesEnPapelera(userMail: String): Flow<List<Cliente>>

    /** Elimina el cliente de la base de datos de forma permanente. */
    @Query("DELETE FROM Cliente_Tabla WHERE id = :clienteId")
    suspend fun eliminarFisico(clienteId: Int)

}