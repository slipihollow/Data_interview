package com.datainterview.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.datainterview.app.data.entity.Activation

@Dao
interface ActivationDao {
    @Insert
    suspend fun insert(activation: Activation): Long

    @Update
    suspend fun update(activation: Activation)

    @Query("SELECT * FROM activations ORDER BY startTime DESC")
    suspend fun getAll(): List<Activation>

    @Query("SELECT * FROM activations WHERE id = :id")
    suspend fun getById(id: Long): Activation?

    @Query("SELECT * FROM activations WHERE status = 'active' LIMIT 1")
    suspend fun getActive(): Activation?

    @Query("SELECT * FROM activations WHERE status = 'scheduled' AND scheduledStart <= :now AND (scheduledEnd IS NULL OR scheduledEnd > :now)")
    suspend fun getScheduledReady(now: Long): List<Activation>

    @Query("DELETE FROM activations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
