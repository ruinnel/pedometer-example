/*
 * Filename	: RunxDatahelper.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PedometerDatahelper extends SQLiteOpenHelper {
  @SuppressWarnings("unused")
  private static final String TAG = "RunxDatahelper";

  private final Context mContext;

  public PedometerDatahelper(Context context) {
    super(context, TableDef.NAME, null, TableDef.VERSION);
    this.mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.setVersion(TableDef.VERSION);

    db.execSQL("CREATE TABLE " + TableDef.History.NAME + " ("
      + TableDef.History._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + TableDef.History.COL_DAY + " integer not null, "
      + TableDef.History.COL_STEPS + " integer not null, "
      + TableDef.History._CREATED + " integer not null"
      + ");"
    );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // TODO delete old DB & create new DB (테이블 정보 변경시. 기존 데이터 마이그레이션 필요)
    Log.i(TAG, "DB Version changed - " + oldVersion + " >> " + newVersion);

    db.execSQL("DROP TABLE " + TableDef.History.NAME);
    onCreate(db);
  }

}
