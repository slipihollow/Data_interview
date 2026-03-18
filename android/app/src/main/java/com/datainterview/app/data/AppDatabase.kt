package com.datainterview.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.datainterview.app.data.dao.ActivationDao
import com.datainterview.app.data.dao.EventDao
import com.datainterview.app.data.entity.Activation
import com.datainterview.app.data.entity.Event

@Database(entities = [Event::class, Activation::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun activationDao(): ActivationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "data_interview.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
