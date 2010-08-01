package com.riotopsys.MALforAndroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MALSqlHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "MALList.db";
	private static final int DATABASE_VERSION = 1;
	private String createAnimeList;

	public MALSqlHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		createAnimeList = context.getString(R.string.anime_table_def);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(createAnimeList);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE `animeList`;");
		onCreate(db);
	}

}