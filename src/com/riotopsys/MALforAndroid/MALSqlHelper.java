package com.riotopsys.MALforAndroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MALSqlHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "MALList.db";
	private static final int DATABASE_VERSION = 2;
	private String createAnimeList;
	private String createMangaList;

	public MALSqlHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		createAnimeList = context.getString(R.string.anime_table_def);
		createMangaList = context.getString(R.string.manga_table_def);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(createAnimeList);
		db.execSQL(createMangaList);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if ( oldVersion == 1){
			db.execSQL(createMangaList);
		}
	}

}