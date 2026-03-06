package com.example.kheprix.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kheprix.data.dao.*
import com.example.kheprix.data.entity.*

@Database(
    entities = [
        Estudo::class,
        Especie::class,
        Variavel::class,
        ValorVariavel::class,
        CampanhaColeta::class,
        UnidadeAmostral::class,
        EventoAmostragem::class,
        RegistroOcorrencia::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun estudoDao(): EstudoDao
    abstract fun especieDao(): EspecieDao
    abstract fun variavelDao(): VariavelDao
    abstract fun valorVariavelDao(): ValorVariavelDao
    abstract fun campanhaColetaDao(): CampanhaColetaDao
    abstract fun unidadeAmostralDao(): UnidadeAmostralDao
    abstract fun eventoAmostragemDao(): EventoAmostragemDao
    abstract fun registroOcorrenciaDao(): RegistroOcorrenciaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
