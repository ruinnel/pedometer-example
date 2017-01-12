/*
 * Filename	: PedometerProvider.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import net.ruinnel.pedometer.db.DatabaseManager;
import net.ruinnel.pedometer.util.Log;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class PedometerProvider extends ContentProvider {
  private static final String TAG = PedometerProvider.class.getSimpleName();

  private DatabaseManager dbManager;

  @Override
  public boolean onCreate() {
    dbManager = new DatabaseManager(getContext());
    return true;
  }

  @Override
  public String getType(Uri uri) {
    // 복수 데이터만 반환.
    return "vnd.android.cursor.dir/";
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    String path = uri.getPath();
    path = (path.indexOf("/") == 0 ? path.substring(1) : path); // cut "/"

    try {
      return dbManager.delete(path, selection, selectionArgs);
    } catch (SQLException e) {
      Log.e(TAG, "SQLException", e);
      return 0;
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    String path = uri.getPath();
    path = (path.indexOf("/") == 0 ? path.substring(1) : path); // cut "/"

    try {
      long rowId = dbManager.insert(path, values);
      if (rowId >= 0) {
        return Uri.parse(uri.toString() + "/" + rowId);
      }
      return null;
    } catch (SQLException e) {
      Log.e(TAG, "SQLException", e);
      return null;
    }
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    String path = uri.getPath();
    path = (path.indexOf("/") == 0 ? path.substring(1) : path); // cut "/"

    try {
      return dbManager.query(path, selection, selectionArgs, sortOrder, null);
    } catch (SQLException e) {
      Log.e(TAG, "SQLException", e);
      return null;
    }
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    String path = uri.getPath();
    path = (path.indexOf("/") == 0 ? path.substring(1) : path); // cut "/"

    try {
      return dbManager.update(path, values, selection, selectionArgs);
    } catch (SQLException e) {
      Log.e(TAG, "SQLException", e);
      return 0;
    }
  }
}
