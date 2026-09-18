package com.company.salesapp.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LocationEventDao _locationEventDao;

  private volatile RouteCacheDao _routeCacheDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `location_events` (`locationEventId` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL, `recordedAt` TEXT NOT NULL, `trackingSessionId` INTEGER, `salesIdLocalRef` TEXT, `syncStatus` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastAttemptAt` INTEGER, `createdAtLocal` INTEGER NOT NULL, PRIMARY KEY(`locationEventId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `route_cache` (`routeDate` TEXT NOT NULL, `source` TEXT, `routeId` INTEGER, `distanceMeters` INTEGER, `durationSeconds` INTEGER, `geometryJson` TEXT NOT NULL, `stopsJson` TEXT NOT NULL, `savedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`routeDate`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fec99e0e7d059e848218bcf68f3fb7e6')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `location_events`");
        db.execSQL("DROP TABLE IF EXISTS `route_cache`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLocationEvents = new HashMap<String, TableInfo.Column>(11);
        _columnsLocationEvents.put("locationEventId", new TableInfo.Column("locationEventId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("accuracy", new TableInfo.Column("accuracy", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("recordedAt", new TableInfo.Column("recordedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("trackingSessionId", new TableInfo.Column("trackingSessionId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("salesIdLocalRef", new TableInfo.Column("salesIdLocalRef", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("lastAttemptAt", new TableInfo.Column("lastAttemptAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationEvents.put("createdAtLocal", new TableInfo.Column("createdAtLocal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocationEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocationEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocationEvents = new TableInfo("location_events", _columnsLocationEvents, _foreignKeysLocationEvents, _indicesLocationEvents);
        final TableInfo _existingLocationEvents = TableInfo.read(db, "location_events");
        if (!_infoLocationEvents.equals(_existingLocationEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "location_events(com.company.salesapp.database.LocationEventEntity).\n"
                  + " Expected:\n" + _infoLocationEvents + "\n"
                  + " Found:\n" + _existingLocationEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsRouteCache = new HashMap<String, TableInfo.Column>(8);
        _columnsRouteCache.put("routeDate", new TableInfo.Column("routeDate", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("source", new TableInfo.Column("source", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("routeId", new TableInfo.Column("routeId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("distanceMeters", new TableInfo.Column("distanceMeters", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("durationSeconds", new TableInfo.Column("durationSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("geometryJson", new TableInfo.Column("geometryJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("stopsJson", new TableInfo.Column("stopsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteCache.put("savedAtEpochMs", new TableInfo.Column("savedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRouteCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRouteCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRouteCache = new TableInfo("route_cache", _columnsRouteCache, _foreignKeysRouteCache, _indicesRouteCache);
        final TableInfo _existingRouteCache = TableInfo.read(db, "route_cache");
        if (!_infoRouteCache.equals(_existingRouteCache)) {
          return new RoomOpenHelper.ValidationResult(false, "route_cache(com.company.salesapp.database.RouteCacheEntity).\n"
                  + " Expected:\n" + _infoRouteCache + "\n"
                  + " Found:\n" + _existingRouteCache);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "fec99e0e7d059e848218bcf68f3fb7e6", "91d6f92fe93f65be94ad2fa31a79ec05");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "location_events","route_cache");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `location_events`");
      _db.execSQL("DELETE FROM `route_cache`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LocationEventDao.class, LocationEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RouteCacheDao.class, RouteCacheDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LocationEventDao locationEventDao() {
    if (_locationEventDao != null) {
      return _locationEventDao;
    } else {
      synchronized(this) {
        if(_locationEventDao == null) {
          _locationEventDao = new LocationEventDao_Impl(this);
        }
        return _locationEventDao;
      }
    }
  }

  @Override
  public RouteCacheDao routeCacheDao() {
    if (_routeCacheDao != null) {
      return _routeCacheDao;
    } else {
      synchronized(this) {
        if(_routeCacheDao == null) {
          _routeCacheDao = new RouteCacheDao_Impl(this);
        }
        return _routeCacheDao;
      }
    }
  }
}
