package com.datainterview.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.datainterview.app.data.entity.Event

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event): Long

    /** Completes an open "application" row by setting its close time. */
    @Query("UPDATE events SET closeTime = :closeTime WHERE id = :id")
    suspend fun updateCloseTime(id: Long, closeTime: String)

    @Query("SELECT * FROM events WHERE activationId = :activationId ORDER BY timestampMillis ASC")
    suspend fun getByActivation(activationId: Long): List<Event>

    @Query("SELECT COUNT(*) FROM events WHERE activationId = :activationId")
    suspend fun countByActivation(activationId: Long): Int

    @Query("DELETE FROM events WHERE activationId = :activationId")
    suspend fun deleteByActivation(activationId: Long)
}
