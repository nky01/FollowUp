package com.followup.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.followup.data.dao.UsuarioDao
import com.followup.data.entity.Usuario
import androidx.room.TypeConverters
import com.followup.data.dao.ClienteDao
import com.followup.data.entity.Cliente

@Database(entities = [Usuario::class], version = 1)
abstract class AppDatabase: RoomDatabase(){

    abstract fun usuarioDao(): UsuarioDao

   companion object{
       @Volatile private var INSTANCIA: AppDatabase?=null
       fun getDatabase(context: Context):AppDatabase{

           return INSTANCIA?:synchronized(this){

               Room.databaseBuilder(
                    context.applicationContext,
                   AppDatabase::class.java,
                    "followup_db"
               ).build().also { INSTANCIA = it }
            }

        }

   }

}