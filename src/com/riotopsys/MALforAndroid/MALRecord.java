package com.riotopsys.MALforAndroid;

import java.io.Serializable;

import android.database.sqlite.SQLiteDatabase;

public abstract class MALRecord implements Serializable{

	private static final long serialVersionUID = 4258327385014269262L;
	public static final int CLEAN = 0;
	public static final int UPDATING = 1;
	public static final int UNSYNCED = 2;
	public static final int DELETED = 3;
		
	public long id;
	public int score;
	public String watchedStatus;
	public int dirty;
	
	public String title;
	
	public String imageUrl;
	
	public String status;
	
	public String memberScore;
	public String rank;
	public String synopsis;
	public String type;

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