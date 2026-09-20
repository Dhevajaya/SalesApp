package com.company.salesapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

/**
 * version 1 -> 2 : penambahan tabel `route_cache` (offline route caching, Section 22 & 25).
 *
 * PERBAIKAN AUDIT: fallbackToDestructiveMigration() DIHAPUS - sebelumnya
 * setiap kenaikan versi DB akan MENGHAPUS SELURUH antrian lokasi offline
 * Sales yang belum ter-sync (location_events), yang justru bertentangan
 * dengan tujuan Local Queue itu sendiri (Section 12: jangan pernah
 * kehilangan data lokasi). Sekarang pakai Migration resmi (MIGRATION_1_2)
 * yang HANYA menambah tabel baru, tidak menyentuh data existing.
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

        /**
         * Skema harus persis sama dengan @Entity RouteCacheEntity di atas
         * (kolom Kotlin non-nullable -> SQL NOT NULL, nullable -> boleh NULL).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `route_cache` (
                        `routeDate` TEXT NOT NULL,
                        `source` TEXT,
                        `routeId` INTEGER,
                        `distanceMeters` INTEGER,
                        `durationSeconds` INTEGER,
                        `geometryJson` TEXT NOT NULL,
                        `stopsJson` TEXT NOT NULL,
                        `savedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`routeDate`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salesapp.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
