package com.riotopsys.MALforAndroid;

import java.io.Serializable;

import android.database.sqlite.SQLiteDatabase;

public abstract class MALRecord implements Serializable{

	public static final int CLEAN = 0;
	public static final int UPDATING = 1;
	public static final int UNSYNCED = 2;
	public static final int DELETED = 3;

	public abstract void pushToDB(SQLiteDatabase db);

	public abstract void pullFromDB(long id, SQLiteDatabase db) throws Exception;
	
	public MALRecord() {
		super();
	}

	protected String addQuotes(String s) {
		String result;
		if (s != null) {
			result = "'" + s.replace("'", "''") + "'";
		} else {
			result = "NULL";
		}
		return result;
	}

}