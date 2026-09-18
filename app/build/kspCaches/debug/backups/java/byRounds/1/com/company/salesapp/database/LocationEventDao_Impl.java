package com.company.salesapp.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocationEventDao_Impl implements LocationEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocationEventEntity> __insertionAdapterOfLocationEventEntity;

  private final EntityDeletionOrUpdateAdapter<LocationEventEntity> __updateAdapterOfLocationEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkAttempt;

  private final SharedSQLiteStatement __preparedStmtOfPurgeSynced;

  public LocationEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocationEventEntity = new EntityInsertionAdapter<LocationEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `location_events` (`locationEventId`,`latitude`,`longitude`,`accuracy`,`recordedAt`,`trackingSessionId`,`salesIdLocalRef`,`syncStatus`,`retryCount`,`lastAttemptAt`,`createdAtLocal`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationEventEntity entity) {
        statement.bindString(1, entity.getLocationEventId());
        statement.bindDouble(2, entity.getLatitude());
        statement.bindDouble(3, entity.getLongitude());
        statement.bindDouble(4, entity.getAccuracy());
        statement.bindString(5, entity.getRecordedAt());
        if (entity.getTrackingSessionId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getTrackingSessionId());
        }
        if (entity.getSalesIdLocalRef() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSalesIdLocalRef());
        }
        statement.bindString(8, entity.getSyncStatus());
        statement.bindLong(9, entity.getRetryCount());
        if (entity.getLastAttemptAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastAttemptAt());
        }
        statement.bindLong(11, entity.getCreatedAtLocal());
      }
    };
    this.__updateAdapterOfLocationEventEntity = new EntityDeletionOrUpdateAdapter<LocationEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `location_events` SET `locationEventId` = ?,`latitude` = ?,`longitude` = ?,`accuracy` = ?,`recordedAt` = ?,`trackingSessionId` = ?,`salesIdLocalRef` = ?,`syncStatus` = ?,`retryCount` = ?,`lastAttemptAt` = ?,`createdAtLocal` = ? WHERE `locationEventId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationEventEntity entity) {
        statement.bindString(1, entity.getLocationEventId());
        statement.bindDouble(2, entity.getLatitude());
        statement.bindDouble(3, entity.getLongitude());
        statement.bindDouble(4, entity.getAccuracy());
        statement.bindString(5, entity.getRecordedAt());
        if (entity.getTrackingSessionId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getTrackingSessionId());
        }
        if (entity.getSalesIdLocalRef() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSalesIdLocalRef());
        }
        statement.bindString(8, entity.getSyncStatus());
        statement.bindLong(9, entity.getRetryCount());
        if (entity.getLastAttemptAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastAttemptAt());
        }
        statement.bindLong(11, entity.getCreatedAtLocal());
        statement.bindString(12, entity.getLocationEventId());
      }
    };
    this.__preparedStmtOfMarkAttempt = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE location_events SET syncStatus = ?, retryCount = retryCount + 1, lastAttemptAt = ? WHERE locationEventId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfPurgeSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM location_events WHERE syncStatus = 'SYNCED' AND createdAtLocal < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LocationEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocationEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final LocationEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLocationEventEntity.handle(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAttempt(final String id, final String status, final long attemptAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAttempt.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, attemptAt);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfMarkAttempt.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object purgeSynced(final long olderThanEpochMillis,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPurgeSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, olderThanEpochMillis);
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
          __preparedStmtOfPurgeSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingBatch(final int limit,
      final Continuation<? super List<LocationEventEntity>> $completion) {
    final String _sql = "SELECT * FROM location_events WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY createdAtLocal ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocationEventEntity>>() {
      @Override
      @NonNull
      public List<LocationEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationEventId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recordedAt");
          final int _cursorIndexOfTrackingSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "trackingSessionId");
          final int _cursorIndexOfSalesIdLocalRef = CursorUtil.getColumnIndexOrThrow(_cursor, "salesIdLocalRef");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfLastAttemptAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAttemptAt");
          final int _cursorIndexOfCreatedAtLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtLocal");
          final List<LocationEventEntity> _result = new ArrayList<LocationEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationEventEntity _item;
            final String _tmpLocationEventId;
            _tmpLocationEventId = _cursor.getString(_cursorIndexOfLocationEventId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final String _tmpRecordedAt;
            _tmpRecordedAt = _cursor.getString(_cursorIndexOfRecordedAt);
            final Long _tmpTrackingSessionId;
            if (_cursor.isNull(_cursorIndexOfTrackingSessionId)) {
              _tmpTrackingSessionId = null;
            } else {
              _tmpTrackingSessionId = _cursor.getLong(_cursorIndexOfTrackingSessionId);
            }
            final String _tmpSalesIdLocalRef;
            if (_cursor.isNull(_cursorIndexOfSalesIdLocalRef)) {
              _tmpSalesIdLocalRef = null;
            } else {
              _tmpSalesIdLocalRef = _cursor.getString(_cursorIndexOfSalesIdLocalRef);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final Long _tmpLastAttemptAt;
            if (_cursor.isNull(_cursorIndexOfLastAttemptAt)) {
              _tmpLastAttemptAt = null;
            } else {
              _tmpLastAttemptAt = _cursor.getLong(_cursorIndexOfLastAttemptAt);
            }
            final long _tmpCreatedAtLocal;
            _tmpCreatedAtLocal = _cursor.getLong(_cursorIndexOfCreatedAtLocal);
            _item = new LocationEventEntity(_tmpLocationEventId,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpRecordedAt,_tmpTrackingSessionId,_tmpSalesIdLocalRef,_tmpSyncStatus,_tmpRetryCount,_tmpLastAttemptAt,_tmpCreatedAtLocal);
            _result.add(_item);
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
  public Object countPending(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM location_events WHERE syncStatus IN ('PENDING', 'FAILED')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
