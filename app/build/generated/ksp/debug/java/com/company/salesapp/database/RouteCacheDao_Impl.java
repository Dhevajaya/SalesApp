package com.company.salesapp.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RouteCacheDao_Impl implements RouteCacheDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RouteCacheEntity> __insertionAdapterOfRouteCacheEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearOtherThan;

  public RouteCacheDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRouteCacheEntity = new EntityInsertionAdapter<RouteCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `route_cache` (`routeDate`,`source`,`routeId`,`distanceMeters`,`durationSeconds`,`geometryJson`,`stopsJson`,`savedAtEpochMs`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RouteCacheEntity entity) {
        statement.bindString(1, entity.getRouteDate());
        if (entity.getSource() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSource());
        }
        if (entity.getRouteId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getRouteId());
        }
        if (entity.getDistanceMeters() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getDistanceMeters());
        }
        if (entity.getDurationSeconds() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getDurationSeconds());
        }
        statement.bindString(6, entity.getGeometryJson());
        statement.bindString(7, entity.getStopsJson());
        statement.bindLong(8, entity.getSavedAtEpochMs());
      }
    };
    this.__preparedStmtOfClearOtherThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM route_cache WHERE routeDate != ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final RouteCacheEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRouteCacheEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearOtherThan(final String keepDate,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearOtherThan.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, keepDate);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearOtherThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final String routeDate,
      final Continuation<? super RouteCacheEntity> $completion) {
    final String _sql = "SELECT * FROM route_cache WHERE routeDate = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, routeDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RouteCacheEntity>() {
      @Override
      @Nullable
      public RouteCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRouteDate = CursorUtil.getColumnIndexOrThrow(_cursor, "routeDate");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfRouteId = CursorUtil.getColumnIndexOrThrow(_cursor, "routeId");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceMeters");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfGeometryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "geometryJson");
          final int _cursorIndexOfStopsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "stopsJson");
          final int _cursorIndexOfSavedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "savedAtEpochMs");
          final RouteCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRouteDate;
            _tmpRouteDate = _cursor.getString(_cursorIndexOfRouteDate);
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final Long _tmpRouteId;
            if (_cursor.isNull(_cursorIndexOfRouteId)) {
              _tmpRouteId = null;
            } else {
              _tmpRouteId = _cursor.getLong(_cursorIndexOfRouteId);
            }
            final Integer _tmpDistanceMeters;
            if (_cursor.isNull(_cursorIndexOfDistanceMeters)) {
              _tmpDistanceMeters = null;
            } else {
              _tmpDistanceMeters = _cursor.getInt(_cursorIndexOfDistanceMeters);
            }
            final Integer _tmpDurationSeconds;
            if (_cursor.isNull(_cursorIndexOfDurationSeconds)) {
              _tmpDurationSeconds = null;
            } else {
              _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            }
            final String _tmpGeometryJson;
            _tmpGeometryJson = _cursor.getString(_cursorIndexOfGeometryJson);
            final String _tmpStopsJson;
            _tmpStopsJson = _cursor.getString(_cursorIndexOfStopsJson);
            final long _tmpSavedAtEpochMs;
            _tmpSavedAtEpochMs = _cursor.getLong(_cursorIndexOfSavedAtEpochMs);
            _result = new RouteCacheEntity(_tmpRouteDate,_tmpSource,_tmpRouteId,_tmpDistanceMeters,_tmpDurationSeconds,_tmpGeometryJson,_tmpStopsJson,_tmpSavedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatest(final Continuation<? super RouteCacheEntity> $completion) {
    final String _sql = "SELECT * FROM route_cache ORDER BY savedAtEpochMs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RouteCacheEntity>() {
      @Override
      @Nullable
      public RouteCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRouteDate = CursorUtil.getColumnIndexOrThrow(_cursor, "routeDate");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfRouteId = CursorUtil.getColumnIndexOrThrow(_cursor, "routeId");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceMeters");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfGeometryJson = CursorUtil.getColumnIndexOrThrow(_cursor, "geometryJson");
          final int _cursorIndexOfStopsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "stopsJson");
          final int _cursorIndexOfSavedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "savedAtEpochMs");
          final RouteCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRouteDate;
            _tmpRouteDate = _cursor.getString(_cursorIndexOfRouteDate);
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final Long _tmpRouteId;
            if (_cursor.isNull(_cursorIndexOfRouteId)) {
              _tmpRouteId = null;
            } else {
              _tmpRouteId = _cursor.getLong(_cursorIndexOfRouteId);
            }
            final Integer _tmpDistanceMeters;
            if (_cursor.isNull(_cursorIndexOfDistanceMeters)) {
              _tmpDistanceMeters = null;
            } else {
              _tmpDistanceMeters = _cursor.getInt(_cursorIndexOfDistanceMeters);
            }
            final Integer _tmpDurationSeconds;
            if (_cursor.isNull(_cursorIndexOfDurationSeconds)) {
              _tmpDurationSeconds = null;
            } else {
              _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            }
            final String _tmpGeometryJson;
            _tmpGeometryJson = _cursor.getString(_cursorIndexOfGeometryJson);
            final String _tmpStopsJson;
            _tmpStopsJson = _cursor.getString(_cursorIndexOfStopsJson);
            final long _tmpSavedAtEpochMs;
            _tmpSavedAtEpochMs = _cursor.getLong(_cursorIndexOfSavedAtEpochMs);
            _result = new RouteCacheEntity(_tmpRouteDate,_tmpSource,_tmpRouteId,_tmpDistanceMeters,_tmpDurationSeconds,_tmpGeometryJson,_tmpStopsJson,_tmpSavedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
