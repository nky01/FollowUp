package com.followup.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.followup.data.dao.ClienteDao
import com.followup.data.dao.UsuarioDao
import com.followup.data.dao.VentaDao
import com.followup.data.entity.Cliente
import com.followup.data.entity.Usuario
import com.followup.data.entity.Venta

// tuve que cambiarlo porque me daba error al crear un nuevo cliente, no se si es por el cambio de version o por el cambio de nombre de la tabla, pero lo importante es que ahora funciona
@Database(entities = [Usuario::class, Cliente::class, Venta::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun clienteDao(): ClienteDao
    abstract fun ventaDao(): VentaDao

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "followup_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCIA = it }
            }
        }
    }
}