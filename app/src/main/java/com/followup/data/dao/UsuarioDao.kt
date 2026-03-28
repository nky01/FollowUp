package com.followup.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.followup.data.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM Usuario_Tabla ORDER BY id DESC")
    fun leerUsuarios(): Flow<List<Usuario>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun crearUsuario(usuario: Usuario)

    @Delete
    suspend fun eliminarUsuario(usuario: Usuario)

    @Update
    suspend fun modificarUsuario(usuario: Usuario)

    @Query("SELECT * FROM Usuario_Tabla WHERE mail = :mail LIMIT 1")
    suspend fun obtenerPorMail(mail:String): Usuario?
}