/*
 * Filename	: TableDef.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.db;

import android.provider.BaseColumns;

public final class TableDef implements BaseColumns {
	public static final String NAME = "net.ruinnel.pedometer.db";
	public static final int VERSION = 1;
	
	private static class Base implements BaseColumns {
		public static final String _CREATED = "_created";
	}
	
	public static final class History extends Base {
		public static final String NAME = "history";
		
		public static final String COL_DAY = "day";
		public static final String COL_STEPS = "steps";
	}
}

