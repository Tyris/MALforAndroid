package com.riotopsys.MALforAndroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class MALManager extends IntentService {

	// push single anime record to mal
	public final static String PUSH = "com.riotopsys.MALForAndroid.PUSH";

	// pull single anime record from mal
	public final static String PULL = "com.riotopsys.MALForAndroid.PULL";

	// push all dirty to MAL then pull all from MAL
	public final static String SYNC = "com.riotopsys.MALForAndroid.SYNC";

	// add new anime to DB and MAL then PULLS it
	public final static String ADD = "com.riotopsys.MALForAndroid.ADD";

	// deletes record from MAL and DB
	public final static String REMOVE = "com.riotopsys.MALForAndroid.REMOVE";

	// updates record with new settings
	public final static String CHANGE = "com.riotopsys.MALForAndroid.CHANGE";

	// pulls image
	public final static String IMAGE = "com.riotopsys.MALForAndroid.IMAGE";

	// Schedules next sync
	public final static String SCHEDULE = "com.riotopsys.MALForAndroid.SCHEDULE";

	// signals change to underlying data
	public final static String RELOAD = "com.riotopsys.MALForAndroid.RELOAD";

	// logging definition
	private final static String LOG_NAME = "MALManager.java";

	private String user;
	private String api;
	private String pass;
	private String cred;

	private boolean activeConnection;

	public MALManager() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String s = intent.getAction();
		Bundle b = intent.getExtras();
		MALRecord ar = null;
		if (b != null) {
			ar = (MALRecord) b.getSerializable("media");
		}

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		SQLiteDatabase db = openHelper.getWritableDatabase();

		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		user = perfs.getString("userName", "");
		api = perfs.getString("api", "");
		pass = perfs.getString("passwd", "");

		cred = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.DEFAULT | Base64.NO_WRAP);

		ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		activeConnection = (connect.getNetworkInfo(0).isConnected() || connect.getNetworkInfo(1).isConnected());

		Log.i(LOG_NAME, s);

		if (s.equals(PUSH)) {
			push(db, ar);
		} else if (s.equals(PULL)) {
			pull(db, ar);
		} else if (s.equals(SYNC)) {
			sync(db);
		} else if (s.equals(ADD)) {
			add(db, ar);
		} else if (s.equals(REMOVE)) {
			remove(db, ar);
		} else if (s.equals(CHANGE)) {
			change(db, ar);
		} else if (s.equals(IMAGE)) {
			pullImage(db, ar);
		} else if (s.equals(SCHEDULE)) {
			schedule( true );
		} else {
			Log.i("MALManager", "unknown intent: " + s);
		}
		db.close();
	}

	private void push(SQLiteDatabase db, MALRecord ar) {
		if (activeConnection) {
			long id = ar.id;
			StringBuffer sb = new StringBuffer();
			URL url;
			
			Boolean isAnime = ( ar instanceof AnimeRecord );
			
			try {
				if ( isAnime ){
					url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(id));
				} else {
					url = new URL("http://" + api + "/mangalist/manga/" + String.valueOf(id));
				}

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setDoOutput(true);
				con.setReadTimeout(getResources().getInteger(R.integer.readTimeout));
				con.setConnectTimeout(getResources().getInteger(R.integer.connectTimeout));
				con.setRequestMethod("PUT");
				con.setRequestProperty("Authorization", "Basic " + cred);

				// sb.append( "_method=PUT\n" );
				sb.append("status=").append(ar.watchedStatus);
				if ( isAnime ){
					sb.append("&").append("episodes=").append(String.valueOf(((AnimeRecord)ar).watchedEpisodes));
				} else {
					sb.append("&").append("chapters=").append(String.valueOf(((MangaRecord)ar).chaptersRead));
					sb.append("&").append("volumes=").append(String.valueOf(((MangaRecord)ar).volumesRead));
				}
				sb.append("&").append("score=").append(String.valueOf(ar.score));

				OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

				wr.write(sb.toString());
				wr.flush();
				wr.close();

				if (200 == con.getResponseCode()) {
					ar.dirty = AnimeRecord.CLEAN;
					ar.pushToDB(db);
				}
			} catch (Exception e) {
				errorNotification();
				Log.e(LOG_NAME, "push", e);
			}
		}
	}

	private void pull(SQLiteDatabase db, MALRecord ar) {
		if (activeConnection) {
			if (ar != null) {
				long id = ar.id;
				URL url;
				try {
					// http://mal-api.com/anime/53?format=xml&mine=1
					if ( ar instanceof AnimeRecord ){
						url = new URL("http://" + api + "/anime/" + String.valueOf(id) + "?mine=1");
					} else {
						url = new URL("http://" + api + "/manga/" + String.valueOf(id) + "?mine=1");
					}

					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setReadTimeout(getResources().getInteger(R.integer.readTimeout));
					con.setConnectTimeout(getResources().getInteger(R.integer.connectTimeout));
					con.setRequestProperty("Authorization", "Basic " + cred);

					BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()), 512);
					String line;
					StringBuffer sb = new StringBuffer();
					while ((line = rd.readLine()) != null) {
						sb.append(line);
					}
					rd.close();

					JSONObject raw = new JSONObject(sb.toString());
					
					MALRecord newRec;
					if ( ar instanceof AnimeRecord ){
						newRec = new AnimeRecord(raw);
					} else {
						newRec = new MangaRecord(raw);
					}

					newRec.pushToDB(db);
					reloadSignal();

					// int fred = con.getResponseCode();
				} catch (Exception e) {
					errorNotification();
					Log.e(LOG_NAME, "push", e);
				}
			} else {
				Log.e(LOG_NAME, "null bundle");
			}
		}
	}

	private void sync(SQLiteDatabase db) {
		pushDirty(db);
		db.execSQL(getString(R.string.dirty));// all is dirt		
		syncAnime(db);
		syncManga(db);
		db.execSQL(getString(R.string.clean));// remove the unclean ones
		schedule(false);
	}
		
	private void syncAnime(SQLiteDatabase db) {
		if (activeConnection) {
			NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Intent intent = new Intent(this, main.class);
			PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

			Notification notification = new Notification(R.drawable.icon, "Synchonizing", System.currentTimeMillis());
			notification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.pullAnime), pi);

			notification.flags |= Notification.FLAG_NO_CLEAR;

			mManager.notify(0, notification);

			//pushDirty(db);

			AnimeRecord ar = new AnimeRecord();
			try {

				//db.execSQL(getString(R.string.dirty));// all is dirt

				URL url = new URL("http://" + api + "/animelist/" + user);
				// InputSource in = new InputSource(new
				// InputStreamReader(url.openStream()));

				BufferedReader rd = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()), 512);
				String line;
				StringBuffer sb = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();

				JSONObject raw = new JSONObject(sb.toString());
				JSONArray array = raw.getJSONArray("anime");				
				for (int c = 0; c < array.length(); c++) {
					JSONObject jo = array.getJSONObject(c);

					notification.setLatestEventInfo(this, "MAL for Android: " + String.valueOf(c + 1) + "/" + String.valueOf(array.length()),
							Html.fromHtml(jo.getString("title")).toString(), pi);
					mManager.notify(0, notification);

					try {
						ar.pullFromDB(jo.getInt("id"), db);
						ar.dirty = AnimeRecord.CLEAN;
						ar.watchedStatus = jo.getString("watched_status");
						ar.score = jo.getInt("score");
						ar.watchedEpisodes = jo.getInt("watched_episodes");
						ar.pushToDB(db);
						reloadSignal();
					} catch (Exception e) {
						ar.id = jo.getInt("id");
						pull(db, ar);
					}

				}

				//db.execSQL(getString(R.string.clean));// remove the unclean ones

			} catch (Exception e) {
				mManager.cancelAll();
				errorNotification();
				Log.e(LOG_NAME, "Sync failed", e);
			}
			
		}
		//schedule();
	}
	private void syncManga(SQLiteDatabase db) {
		if (activeConnection) {
			NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Intent intent = new Intent(this, main.class);
			PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

			Notification notification = new Notification(R.drawable.icon, "Synchonizing", System.currentTimeMillis());
			notification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.pullManga), pi);

			notification.flags |= Notification.FLAG_NO_CLEAR;

			mManager.notify(0, notification);

			//pushDirty(db);

			MangaRecord mr = new MangaRecord();
			try {

				//db.execSQL(getString(R.string.dirty));// all is dirt

				URL url = new URL("http://" + api + "/mangalist/" + user);
				// InputSource in = new InputSource(new
				// InputStreamReader(url.openStream()));

				BufferedReader rd = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()), 512);
				String line;
				StringBuffer sb = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();

				JSONObject raw = new JSONObject(sb.toString());
				JSONArray array = raw.getJSONArray("manga");				
				for (int c = 0; c < array.length(); c++) {
					JSONObject jo = array.getJSONObject(c);

					notification.setLatestEventInfo(this, "MAL for Android: " + String.valueOf(c + 1) + "/" + String.valueOf(array.length()),
							Html.fromHtml(jo.getString("title")).toString(), pi);
					mManager.notify(0, notification);

					try {
						mr.pullFromDB(jo.getInt("id"), db);
						mr.dirty = AnimeRecord.CLEAN;
						mr.watchedStatus = jo.getString("read_status");
						mr.score = jo.getInt("score");
						mr.chaptersRead = jo.getInt("chapters_read");
						mr.volumesRead = jo.getInt("volumes_read");
						
						mr.pushToDB(db);
						reloadSignal();
					} catch (Exception e) {
						mr.id = jo.getInt("id");
						pull(db, mr);
					}

				}

				//db.execSQL(getString(R.string.clean));// remove the unclean ones

			} catch (Exception e) {
				errorNotification();
				Log.e(LOG_NAME, "Sync failed", e);
			}

			mManager.cancelAll();
		}
		//schedule();
	}

	private void add(SQLiteDatabase db, MALRecord ar) {
		String ws = ar.watchedStatus;
		if (activeConnection) {
			try {
				URL url;
				if ( ar instanceof AnimeRecord ){
					url = new URL("http://" + api + "/animelist/anime");
				} else {
					url = new URL("http://" + api + "/mangalist/manga");
				}

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setDoOutput(true);
				con.setReadTimeout(getResources().getInteger(R.integer.readTimeout));
				con.setConnectTimeout(getResources().getInteger(R.integer.connectTimeout));
				con.setRequestMethod("POST");
				con.setRequestProperty("Authorization", "Basic " + cred);

				// sb.append( "_method=PUT\n" );
				StringBuffer sb = new StringBuffer();
				if ( ar instanceof AnimeRecord ){
					sb.append("anime_id=");
				} else {
					sb.append("manga_id=");
				}
				sb.append(String.valueOf(ar.id));
				sb.append("&status=").append(ar.watchedStatus);

				OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

				wr.write(sb.toString());
				wr.flush();
				wr.close();

				int fred = con.getResponseCode();
				if (fred == 200) {
					pull(db, ar);
					ar.pullFromDB(ar.id, db);
					ar.watchedStatus = ws;
					ar.pushToDB(db);
					reloadSignal();
				}

			} catch (Exception e) {
				errorNotification();
				Log.e(LOG_NAME, "add", e);
			}
		}
	}

	private void remove(SQLiteDatabase db, MALRecord ar) {
		ar.dirty = AnimeRecord.DELETED;
		ar.pushToDB(db);
		reloadSignal();
		if (activeConnection) {
			try {
				URL url;
				if ( ar instanceof AnimeRecord ){
					url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(ar.id));
				} else {
					url = new URL("http://" + api + "/mangalist/manga/" + String.valueOf(ar.id));
				}

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(getResources().getInteger(R.integer.readTimeout));
				con.setConnectTimeout(getResources().getInteger(R.integer.connectTimeout));
				con.setRequestMethod("DELETE");
				con.setRequestProperty("Authorization", "Basic " + cred);

				con.connect();

				if (con.getResponseCode() == 200) {
					if ( ar instanceof AnimeRecord ){
						db.execSQL("delete from animeList where id = " + String.valueOf(ar.id));
					} else {
						db.execSQL("delete from mangaList where id = " + String.valueOf(ar.id));
					}
				}
			} catch (Exception e) {
				errorNotification();
				Log.e(LOG_NAME, "remove", e);
			}
		}
	}

	private void change(SQLiteDatabase db, MALRecord ar) {
		ar.dirty = AnimeRecord.UNSYNCED;
		ar.pushToDB(db);
		if (activeConnection) {
			push(db, ar);
		}
		reloadSignal();
	}

	private void pullImage(SQLiteDatabase db, MALRecord ar) {
		if (activeConnection) {
			try {
				URL url = new URL(ar.imageUrl);
				URLConnection ucon = url.openConnection();

				File root = Environment.getExternalStorageDirectory();
				File file;
				if ( ar instanceof AnimeRecord ){
					//file = new File(root, "Android/data/com.riotopsys.MALForAndroid/images/anime/" + String.valueOf(ar.id));
					file = new File(root, getString(R.string.imagePathAnime) + String.valueOf(ar.id));
				} else {
					//file = new File(root, "Android/data/com.riotopsys.MALForAndroid/images/manga/" + String.valueOf(ar.id));
					file = new File(root, getString(R.string.imagePathManga) + String.valueOf(ar.id));
				}
				file.mkdirs();
				if (file.exists()) {
					file.delete();
				}
				file.createNewFile();

				ByteArrayBuffer baf = new ByteArrayBuffer(50);

				InputStream is = ucon.getInputStream();

				BufferedInputStream bis = new BufferedInputStream(is, 1024 * 10);

				int current = 0;

				while ((current = bis.read()) != -1) {

					baf.append((byte) current);

				}

				/* Convert the Bytes read to a String. */
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(baf.toByteArray());
				fos.close();
				reloadSignal();
			} catch (Exception e) {
				errorNotification();
				Log.e(LOG_NAME, "Failed on img", e);
			}
		}
	}

	private void pushDirty(SQLiteDatabase db) {
		HttpURLConnection con;
		URL url;

		Cursor c = db.rawQuery("select id, 1 as type from animeList where dirty <> 0 union select id, 2 as type from mangaList where dirty <> 0", null);

		if (c.moveToFirst()) {
			while (!c.isAfterLast()) {

				try {
					MALRecord ar;
					
					if ( c.getInt(c.getColumnIndex("type")) == 1 ) {
						ar = new AnimeRecord(c.getInt(c.getColumnIndex("id")), db);
					} else {
						ar = new MangaRecord(c.getInt(c.getColumnIndex("id")), db);
					}

					switch (ar.dirty) {
						case AnimeRecord.CLEAN:
							Log.wtf(LOG_NAME, "WTF I said no 0");
							break;
						case AnimeRecord.UPDATING:
							// only seen during an update
							break;
						case AnimeRecord.UNSYNCED:
							// just a change
							push(db, ar);
							break;
						case AnimeRecord.DELETED:
							// delete
							try {

								url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(ar.id));

								con = (HttpURLConnection) url.openConnection();
								con.setReadTimeout(getResources().getInteger(R.integer.readTimeout));
								con.setConnectTimeout(getResources().getInteger(R.integer.connectTimeout));
								con.setRequestMethod("DELETE");
								con.setRequestProperty("Authorization", "Basic " + cred);

								con.connect();

								if (con.getResponseCode() == 200) {
									db.execSQL("delete from animeList where id = " + String.valueOf(ar.id));
								}

							} catch (Exception e) {
								errorNotification();
								Log.e(LOG_NAME, "push dirty", e);
							}
							break;
					}
					c.moveToNext();
				} catch (Exception e) {
					Log.e(LOG_NAME, "push dirty: ar not found", e);
				}
			}
		}
		
		c.close();
		reloadSignal();
	}

	private void reloadSignal() {
		Intent i = new Intent(RELOAD);
		sendBroadcast(i);
	}

	private void schedule(boolean createNew) {
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

		int interval = Integer.parseInt(perfs.getString("updateFreq", "14400000"));
		long firstTime = SystemClock.elapsedRealtime() + interval;

		Intent i = new Intent(this, MALManager.class);
		i.setAction(SYNC);
		PendingIntent mAlarmSender = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE);
		if (mAlarmSender == null ) {
			mAlarmSender = PendingIntent.getService(this, 0, i, 0);
			
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, interval, mAlarmSender);

			Log.i(LOG_NAME, "schedule set");
		} else if ( createNew ){
			
			am.cancel(mAlarmSender);
			
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, interval, mAlarmSender);
			
		}
	}

	public static AnimeRecord getAnime(long id, Context c) {

		MALSqlHelper openHelper = new MALSqlHelper(c);
		SQLiteDatabase db = openHelper.getReadableDatabase();

		AnimeRecord ar;
		try {
			ar = new AnimeRecord(id, db);
		} catch (Exception e) {
			ar = null;
		}

		db.close();
		return ar;
	}
	
	public static MangaRecord getManga(long id, Context c) {

		MALSqlHelper openHelper = new MALSqlHelper(c);
		SQLiteDatabase db = openHelper.getReadableDatabase();

		MangaRecord ar;
		try {
			ar = new MangaRecord(id, db);
		} catch (Exception e) {
			ar = null;
		}

		db.close();
		return ar;
	}

	public static boolean verifyCredentials(Context c) {
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(c);
		String user = perfs.getString("userName", "");
		String api = perfs.getString("api", "");
		String pass = perfs.getString("passwd", "");

		String cred = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.DEFAULT | Base64.NO_WRAP);

		Boolean result = false;

		try {
			URL url = new URL("http://" + api + "/account/verify_credentials");

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(10000 /* milliseconds */);
			con.setConnectTimeout(15000 /* milliseconds */);
			con.setRequestProperty("Authorization", "Basic " + cred);

			con.connect();

			result = (con.getResponseCode() == 200);

		} catch (Exception e) {
			Toast.makeText(c, R.string.connectError, Toast.LENGTH_SHORT).show();
			Log.e(LOG_NAME, "verifyCredentials", e);
		}

		return result;

	}

	private void errorNotification() {
		NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(this, main.class);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

		Notification notification = new Notification(R.drawable.icon, getResources().getString(R.string.connectError), System.currentTimeMillis());
		notification.setLatestEventInfo(this, "MAL for Android", getResources().getString(R.string.connectError), pi);
		
		mManager.notify(0, notification);
	}

}
