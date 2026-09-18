package com.company.salesapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * version 1 -> 2 : penambahan tabel `route_cache` (offline route caching, Section 22 & 25).
 *
 * exportSchema di-set false selama fase development supaya Gradle tidak perlu
 * folder schema. Aktifkan kembali (+ set room.schemaLocation di build.gradle)
 * sebelum rilis produksi, bersamaan dengan penggantian
 * fallbackToDestructiveMigration() menjadi Migration resmi.
 */
@Database(
    entities = [LocationEventEntity::class, RouteCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationEventDao(): LocationEventDao

    abstract fun routeCacheDao(): RouteCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salesapp.db"
                )
                    // PERINGATAN: destructive migration MENGHAPUS antrian lokasi yang
                    // belum ter-sync bila versi DB naik. Aman selama app belum dipakai
                    // Sales beneran di lapangan. Ganti dengan Migration resmi sebelum rilis.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
