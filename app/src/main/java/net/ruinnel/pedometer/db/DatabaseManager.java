/*
 * Filename	: DatabaseManager.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.common.collect.Lists;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Utils;

import java.util.Date;
import java.util.List;

public class DatabaseManager {

  private int defaultConflict = SQLiteDatabase.CONFLICT_NONE;
  private SQLiteDatabase db = null;
  private PedometerDatahelper dbHelper = null;

  public DatabaseManager(Context context) {
    this.dbHelper = new PedometerDatahelper(context);
    this.db = this.dbHelper.getWritableDatabase();
  }

  /**
   * @param conflictAlgorithm - use in android.database.sqlite.SQLiteDatabase.CONFLICT_*
   */
  public void setConflictAlgorithm(int conflictAlgorithm) {
    this.defaultConflict = conflictAlgorithm;
  }

  // method for select
  public Cursor query(String table, String selection, String[] selectionArgs, String orderBy, String limit) {
    return db.query(table, null, selection, selectionArgs, null, null, orderBy, limit);
  }

  // method for insert
  public long insert(String table, ContentValues values) {
    return db.insertWithOnConflict(table, null, values, defaultConflict);
  }

  // method for delete
  public int delete(String table, String whereClause, String[] whereArgs) {
    return db.delete(table, whereClause, whereArgs);
  }

  // method for update
  public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
    return db.update(table, values, whereClause, whereArgs);
  }

  public List<History> getHistories(Date start, Date end, Integer offset, Integer count) {
    List<History> histories = Lists.newArrayList();
    String selection = null;
    String[] selectionArgs = null;
    if (start != null && end != null) {
      selection = String.format("%s >= ? AND %s <= ? ", TableDef.History.COL_DAY, TableDef.History.COL_DAY);
      selectionArgs = new String[]{String.valueOf(start.getTime()), String.valueOf(end.getTime())};
    }

    String orderBy = TableDef.History.COL_DAY + " desc";
    String limit = null;
    if (offset != null && count != null) {
      limit = "";
    }
    Cursor cursor = this.query(TableDef.History.NAME, selection, selectionArgs, orderBy, limit);
    while (cursor.moveToNext()) {
      long temp;
      History history = new History();
      history.id = cursor.getLong(cursor.getColumnIndex(TableDef.History._ID));

      temp = cursor.getLong(cursor.getColumnIndex(TableDef.History.COL_DAY));
      history.day = new Date(temp);

      history.steps = cursor.getInt(cursor.getColumnIndex(TableDef.History.COL_STEPS));

      temp = cursor.getLong(cursor.getColumnIndex(TableDef.History._CREATED));
      history.created = new Date(temp);

      histories.add(history);
    }
    cursor.close();
    return histories;
  }

  public History todayHistory() {
    History history = null;
    String selection = String.format("%s = ?", TableDef.History.COL_DAY);
    String[] selectionArgs = new String[]{String.valueOf(Utils.getToday().getTime())};
    String limit = "1";
    Cursor cursor = this.query(TableDef.History.NAME, selection, selectionArgs, null, limit);
    while (cursor.moveToNext()) {
      long temp;
      history = new History();
      history.id = cursor.getLong(cursor.getColumnIndex(TableDef.History._ID));

      temp = cursor.getLong(cursor.getColumnIndex(TableDef.History.COL_DAY));
      history.day = new Date(temp);

      history.steps = cursor.getInt(cursor.getColumnIndex(TableDef.History.COL_STEPS));

      temp = cursor.getLong(cursor.getColumnIndex(TableDef.History._CREATED));
      history.created = new Date(temp);
    }
    return history;
  }

  public void saveHistory(History history) {
    ContentValues vals = new ContentValues();
    vals.put(TableDef.History.COL_DAY, history.day.getTime());
    vals.put(TableDef.History.COL_STEPS, history.steps);
    vals.put(TableDef.History._CREATED, (history.created != null ? history.created.getTime() : System.currentTimeMillis()));
    this.insert(TableDef.History.NAME, vals);
  }

  public void deleteHistory(History history) {
    String where = TableDef.History._ID + " = " + history.id;
    this.delete(TableDef.History.NAME, where, null);
  }

  public void updateHistory(History history) {
    String where = String.format("%s = ?", TableDef.History.COL_DAY);
    String[] whereArgs = new String[]{String.valueOf(history.day.getTime())};
    ContentValues vals = new ContentValues();
    vals.put(TableDef.History.COL_STEPS, history.steps);
    this.update(TableDef.History.NAME, vals, where, whereArgs);
  }
}
