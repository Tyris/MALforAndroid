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
import android.util.Base64;
import android.util.Log;

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
		AnimeRecord ar = null;
		if (b != null) {
			ar = (AnimeRecord) b.getSerializable("anime");
		}

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		SQLiteDatabase db = openHelper.getWritableDatabase();

		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		user = perfs.getString("userName", "");
		api = perfs.getString("api", "");
		pass = perfs.getString("passwd", "");

		/*
		 * String cred2; cred = android.util.Base64.encodeToString((user + ":" +
		 * pass).getBytes(), android.util.Base64.DEFAULT |
		 * android.util.Base64.NO_WRAP); cred2 = Base64.encodeBytes((user + ":"
		 * + pass).getBytes());
		 */

		cred = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.DEFAULT | Base64.NO_WRAP);

		ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		activeConnection = (connect.getNetworkInfo(0).isConnected() || connect.getNetworkInfo(1).isConnected());

		Log.i(LOG_NAME, s);

		if (s.equals(PUSH)) {
			push(db, ar);
		} else if (s.equals(PULL)) {
			pull(db, ar);
			reloadSignal();
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
			schedule();
		} else {
			Log.i("MALManager", "unknown intent: " + s);
		}
		db.close();
	}

	private void push(SQLiteDatabase db, AnimeRecord ar) {
		if (activeConnection) {
			long id = ar.id;
			StringBuffer sb = new StringBuffer();
			URL url;
			try {
				url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(id));

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setDoOutput(true);
				con.setReadTimeout(10000);
				con.setConnectTimeout(15000);
				con.setRequestMethod("PUT");
				con.setRequestProperty("Authorization", "Basic " + cred);

				// sb.append( "_method=PUT\n" );
				sb.append("status=").append(ar.status);
				sb.append("&").append("episodes=").append(String.valueOf(ar.watchedEpisodes));
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
				Log.e(LOG_NAME, "push", e);
			}
		}
	}

	private void pull(SQLiteDatabase db, AnimeRecord ar) {
		if (activeConnection) {
			if (ar != null) {
				long id = ar.id;
				URL url;
				try {
					// http://mal-api.com/anime/53?format=xml&mine=1
					url = new URL("http://" + api + "/anime/" + String.valueOf(id) + "?mine=1");

					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setReadTimeout(10000);
					con.setConnectTimeout(15000);
					con.setRequestProperty("Authorization", "Basic " + cred);

					BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()), 512);
					String line;
					StringBuffer sb = new StringBuffer();
					while ((line = rd.readLine()) != null) {
						sb.append(line);
					}
					rd.close();

					JSONObject raw = new JSONObject(sb.toString());

					AnimeRecord ar2 = new AnimeRecord(raw);

					ar2.pushToDB(db);
					reloadSignal();

					// int fred = con.getResponseCode();
				} catch (Exception e) {
					Log.e(LOG_NAME, "push", e);
				}
			} else {
				Log.e(LOG_NAME, "null bundle");
			}
		}
	}

	private void sync(SQLiteDatabase db) {
		if (activeConnection) {
			NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Intent intent = new Intent(this, main.class);
			PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

			Notification notification = new Notification(R.drawable.icon, "Synchonizing", System.currentTimeMillis());
			notification.setLatestEventInfo(this, "MAL for Android", "Pulling anime list from MAL", pi);

			notification.flags |= Notification.FLAG_NO_CLEAR;
			mManager.notify(0, notification);

			pushDirty(db);

			AnimeRecord ar = new AnimeRecord();
			try {

				db.execSQL(getString(R.string.dirty));// all is dirt

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
					ar.id = jo.getInt("id");
					pull(db, ar);
					Log.i(LOG_NAME, "Sync: " + String.valueOf(c) + "/" + String.valueOf(array.length()));
				}

				db.execSQL(getString(R.string.clean));// remove the unclean ones

			} catch (Exception e) {
				Log.e(LOG_NAME, "Sync failed", e);
			}

			mManager.cancelAll();
		}
		schedule();
	}

	private void add(SQLiteDatabase db, AnimeRecord ar) {
		if (activeConnection) {
			try {
				URL url = new URL("http://" + api + "/animelist/anime");

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setDoOutput(true);
				con.setReadTimeout(10000);
				con.setConnectTimeout(15000);
				con.setRequestMethod("POST");
				con.setRequestProperty("Authorization", "Basic " + cred);

				// sb.append( "_method=PUT\n" );
				StringBuffer sb = new StringBuffer();
				sb.append("anime_id=").append(String.valueOf(ar.id));
				sb.append("&status=").append(ar.watchedStatus);

				OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

				wr.write(sb.toString());
				wr.flush();
				wr.close();

				int fred = con.getResponseCode();
				if (fred == 200) {
					pull(db, ar);
				}

			} catch (Exception e) {
				Log.e(LOG_NAME, "add", e);
			}
		}
	}

	private void remove(SQLiteDatabase db, AnimeRecord ar) {
		ar.dirty = AnimeRecord.DELETED;
		reloadSignal();
		if (activeConnection) {
			try {
				URL url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(ar.id));

				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(10000 /* milliseconds */);
				con.setConnectTimeout(15000 /* milliseconds */);
				con.setRequestMethod("DELETE");
				con.setRequestProperty("Authorization", "Basic " + cred);

				con.connect();

				if (con.getResponseCode() == 200) {
					db.execSQL("delete from animeList where id = " + String.valueOf(ar.id));
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "remove", e);
			}
		}
	}

	private void change(SQLiteDatabase db, AnimeRecord ar) {
		ar.dirty = AnimeRecord.UNSYNCED;
		ar.pushToDB(db);
		reloadSignal();
		if (activeConnection) {
			push(db, ar);
		}
	}

	private void pullImage(SQLiteDatabase db, AnimeRecord ar) {
		if (activeConnection) {
			try {
				URL url = new URL(ar.imageUrl);
				URLConnection ucon = url.openConnection();

				File root = Environment.getExternalStorageDirectory();
				File file = new File(root, "Android/data/com.riotopsys.MALForAndroid/images/" + String.valueOf(ar.id));
				file.mkdirs();
				if (file.exists()) {
					file.delete();
				}
				file.createNewFile();

				ByteArrayBuffer baf = new ByteArrayBuffer(50);

				InputStream is = ucon.getInputStream();

				BufferedInputStream bis = new BufferedInputStream(is);

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
				Log.e(LOG_NAME, "Failed on img", e);
			}
		}
	}

	private void pushDirty(SQLiteDatabase db) {
		HttpURLConnection con;
		URL url;

		Cursor c = db.rawQuery("select id from animeList where dirty <> 0;", null);

		if (c.moveToFirst()) {
			while (!c.isAfterLast()) {

				AnimeRecord ar = new AnimeRecord(c.getInt(c.getColumnIndex("id")), db);

				switch (ar.dirty) {
					case AnimeRecord.CLEAN:
						Log.e(LOG_NAME, "WTF i said no 0");
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
							con.setReadTimeout(10000 /* milliseconds */);
							con.setConnectTimeout(15000 /* milliseconds */);
							con.setRequestMethod("DELETE");
							con.setRequestProperty("Authorization", "Basic " + cred);

							con.connect();

							if (con.getResponseCode() == 200) {
								db.execSQL("delete from animeList where id = " + String.valueOf(ar.id));
							}

						} catch (Exception e) {
							Log.e(LOG_NAME, "push dirty", e);
						}
						break;
				}
				c.moveToNext();
			}
		}
		c.close();
		reloadSignal();
	}

	private void reloadSignal() {
		Intent i = new Intent(RELOAD);
		sendBroadcast(i);
	}

	private void schedule() {
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);

		int interval = Integer.parseInt(perfs.getString("updateFreq", "360000"));

		Intent i = new Intent(this, MALManager.class);
		i.setAction(Intent.ACTION_SYNC);
		PendingIntent mAlarmSender = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE);
		if (mAlarmSender == null) {
			mAlarmSender = PendingIntent.getService(this, 0, i, 0);
			long firstTime = SystemClock.elapsedRealtime() + interval;

			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, interval, mAlarmSender);

			Log.e(LOG_NAME, "schedule set");
		}
	}

	public static AnimeRecord getAnime(long id, Context c) {

		MALSqlHelper openHelper = new MALSqlHelper(c);
		SQLiteDatabase db = openHelper.getReadableDatabase();

		AnimeRecord ar = new AnimeRecord(id, db);

		db.close();
		return ar;
	}

}
