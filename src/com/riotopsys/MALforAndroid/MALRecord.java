package com.riotopsys.MALforAndroid;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.util.Log;

public abstract class MALRecord implements Serializable {

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

			if (raw.isNull("score")) {
				score = 0;
			} else {
				score = raw.getInt("score");
			}

			title = Html.fromHtml(raw.getString("title")).toString();
			type = Html.fromHtml(raw.getString("type")).toString();
			imageUrl = Html.fromHtml(raw.getString("image_url")).toString();
			status = Html.fromHtml(raw.getString("status")).toString();
			// watchedStatus =
			// Html.fromHtml(raw.getString("watched_status")).toString();

			rank = Html.fromHtml(raw.getString("rank")).toString();
			memberScore = Html.fromHtml(raw.getString("members_score")).toString();
			synopsis = Html.fromHtml(raw.getString("synopsis")).toString();
			if (rank.equals("null")) {
				rank = null;
			}

			if (memberScore.equals("null")) {
				memberScore = null;
			}

			if (synopsis.equals("null")) {
				synopsis = null;
			}

			dirty = CLEAN;
		} catch (Exception e) {
			Log.e(LOG_NAME, "JSON failed", e);
		}
	}

	public abstract void pushToDB(SQLiteDatabase db);

	protected LinkedList<MALRecord> loadRelated(String type, JSONObject raw) {
		LinkedList<MALRecord> result = new LinkedList<MALRecord>();
		MALRecord temp;
		try {
			JSONArray related = raw.getJSONArray(type);

			for (int c = 0; c < related.length(); c++) {
				JSONObject item = related.getJSONObject(c);
				if (item.has("anime_id")) {
					temp = new AnimeRecord();
					temp.id = item.getLong("anime_id");
				} else {
					temp = new MangaRecord();
					temp.id = item.getLong("manga_id");
				}
				temp.title = Html.fromHtml(item.getString("title")).toString();
				result.add(temp);
			}
		} catch (JSONException e) {
			// not found no biggie.
		}
		return result;
	}

	protected LinkedList<MALRecord> loadRelated(String type, SQLiteDatabase db) {
		LinkedList<MALRecord> result = new LinkedList<MALRecord>();
		String recordType;
		MALRecord temp;
		@SuppressWarnings("rawtypes")
		Class fred;

		if (this instanceof AnimeRecord) {
			recordType = "Anime";
		} else {
			recordType = "Manga";
		}

		if (type.equals("manga_adaptation") || type.equals("related_manga")) {
			fred = MangaRecord.class;
		} else {
			fred = AnimeRecord.class;
		}

		Cursor c = db
				.rawQuery("select * from related where parentId = '" + id + "' and parentType = '" + recordType + "' and category = '" + type + "' ", null);

		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			try {
				temp = (MALRecord) (fred.newInstance());
				temp.id = c.getLong(c.getColumnIndex("id"));
				temp.title = c.getString(c.getColumnIndex("title"));
				result.add(temp);
			} catch (Exception e) {
				Log.e(LOG_NAME, "WTF!", e);
			}
		}

		c.close();

		return result;
	}

	protected void saveRelated(String type, SQLiteDatabase db, LinkedList<MALRecord> related) {
		if (!related.isEmpty()) {
			String recordType;
			if (this instanceof AnimeRecord) {
				recordType = "Anime";
			} else {
				recordType = "Manga";
			}

			String cmd = "delete from related where parentId = '" + id + "' and parentType = '" + recordType + "' and category = '" + type + "' ";
			db.execSQL(cmd);
			for (MALRecord record : related) {
				cmd = "insert into related values ( '" + id + "', '" + recordType + "', '" + type + "', '" + record.id + "', '" + record.title + "' )";
				db.execSQL(cmd);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		boolean result = (o instanceof MALRecord);
		if (result) {
			result &= (id == ((MALRecord) o).id);
			result &= (score == ((MALRecord) o).score);

			result &= watchedStatus.equals(((MALRecord) o).watchedStatus);
			result &= title.equals(((MALRecord) o).title);
			result &= imageUrl.equals(((MALRecord) o).imageUrl);
			result &= status.equals(((MALRecord) o).status);
			result &= type.equals(((MALRecord) o).type);

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