package com.company.salesapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RouteCacheEntity)

    @Query("SELECT * FROM route_cache WHERE routeDate = :routeDate LIMIT 1")
    suspend fun getByDate(routeDate: String): RouteCacheEntity?

    @Query("SELECT * FROM route_cache ORDER BY savedAtEpochMs DESC LIMIT 1")
    suspend fun getLatest(): RouteCacheEntity?

    /** Buang cache rute hari-hari sebelumnya supaya DB tidak membengkak. */
    @Query("DELETE FROM route_cache WHERE routeDate != :keepDate")
    suspend fun clearOtherThan(keepDate: String)
}
