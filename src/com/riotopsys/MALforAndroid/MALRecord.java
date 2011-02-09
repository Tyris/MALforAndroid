package com.riotopsys.MALforAndroid;

import java.io.Serializable;

import org.json.JSONObject;

import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.util.Log;

public abstract class MALRecord implements Serializable{

	private final static String LOG_NAME = "MALRecord.java";
	
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

	public MALRecord(JSONObject raw) {
		try {			
			id = raw.getInt("id");
								
			if ( raw.isNull("score") ){
				score = 0;
			} else {
				score = raw.getInt("score");
			}
			
			title = Html.fromHtml(raw.getString("title")).toString();
			type = Html.fromHtml(raw.getString("type")).toString();
			imageUrl = Html.fromHtml(raw.getString("image_url")).toString();
			status = Html.fromHtml(raw.getString("status")).toString();			
			//watchedStatus = Html.fromHtml(raw.getString("watched_status")).toString();
			
			rank = Html.fromHtml(raw.getString("rank")).toString();
			memberScore = Html.fromHtml(raw.getString("members_score")).toString();			
			synopsis = Html.fromHtml(raw.getString("synopsis")).toString();
			if ( rank.equals("null")){
				rank = null;
			}
			
			if ( memberScore.equals("null")){
				memberScore = null;
			}
			
			if ( synopsis.equals("null")){
				synopsis = null;
			}
			

			dirty = CLEAN;
		} catch (Exception e) {
			Log.e(LOG_NAME, "JSON failed", e);
		}
	}

	public abstract void pushToDB(SQLiteDatabase db);
	
	@Override
	public boolean equals( Object o ){
		boolean result = (o instanceof MALRecord);
		if ( result  ){
			result &= (id == ((MALRecord)o).id) ;
			result &= (score == ((MALRecord)o).score);

			result &= watchedStatus.equals(((MALRecord)o).watchedStatus);
			result &= title.equals(((MALRecord)o).title);
			result &= imageUrl.equals(((MALRecord)o).imageUrl);
			result &= status.equals(((MALRecord)o).status);
			result &= type.equals(((MALRecord)o).type);
			
			//result &= memberScore.equals(((MALRecord)o).memberScore);
			//result &= rank.equals(((MALRecord)o).rank);
			//result &= synopsis.equals(((MALRecord)o).synopsis);
						
		}		
		return result;
	}

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