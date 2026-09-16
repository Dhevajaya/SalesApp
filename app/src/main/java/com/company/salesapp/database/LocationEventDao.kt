package com.company.salesapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: LocationEventEntity)

    @Update
    suspend fun update(event: LocationEventEntity)

    @Query("SELECT * FROM location_events WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY createdAtLocal ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int = 50): List<LocationEventEntity>

    @Query("SELECT COUNT(*) FROM location_events WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun countPending(): Int

    @Query("UPDATE location_events SET syncStatus = :status, retryCount = retryCount + 1, lastAttemptAt = :attemptAt WHERE locationEventId = :id")
    suspend fun markAttempt(id: String, status: String, attemptAt: Long)

    @Query("DELETE FROM location_events WHERE syncStatus = 'SYNCED' AND createdAtLocal < :olderThanEpochMillis")
    suspend fun purgeSynced(olderThanEpochMillis: Long)
}
