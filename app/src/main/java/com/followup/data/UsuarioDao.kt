package com.followup.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow // tarea que se ejecuta asincronicamente

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM Usuario ORDER BY id DESC")
    fun leerUsuarios():Flow<List<Usuario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun crearUsuario(usuario:Usuario)

    @Delete
    suspend fun eliminarUsuario(usuario:Usuario)

    @Update
    suspend fun modificarUsuario(usuario:Usuario)

    @Query("SELECT * FROM Usuario WHERE mail = :mail LIMIT 1")
    suspend fun obtenerPorMail(mail:String):Usuario?
}