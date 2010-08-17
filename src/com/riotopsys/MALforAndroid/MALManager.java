package com.riotopsys.MALforAndroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.util.ByteArrayBuffer;
import org.xml.sax.InputSource;

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
import android.util.Log;

public class MALManager extends IntentService {

	public MALManager() {
		super("MALManager");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String s = intent.getAction();

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		SQLiteDatabase db = openHelper.getWritableDatabase();

		ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		Log.i("MALManager", s);

		if (s.equals(Intent.ACTION_SEND)) {
			pushDirty(db);
			// send dirty to MAL
		} else if (s.equals(Intent.ACTION_SYNC)) {
			// get list from MAL
			if (connect.getNetworkInfo(0).isConnected() || connect.getNetworkInfo(1).isConnected()) {
				Log.i("MALManager", "syncing");
				pushDirty(db);
				pullList(db);
				schedule();
			}
		} else if (s.equals("SCHEDULE")) {
			schedule();
		} else if (s.equals("com.riotopsys.MALForAndroid.FETCH_EXTRAS")) {
			if (connect.getNetworkInfo(0).isConnected() || connect.getNetworkInfo(1).isConnected()) {
				Bundle b = intent.getExtras();
				long id = b.getLong("id", 0);
				pullExtras(db, id);
				// pullImage(db, id);
			}
		} else if (s.equals("com.riotopsys.MALForAndroid.IMAGE")) {
			if (connect.getNetworkInfo(0).isConnected() || connect.getNetworkInfo(1).isConnected()) {
				Bundle b = intent.getExtras();
				long id = b.getLong("id", 0);
				pullImage(db, id);
			}
		} else if (s.equals("com.riotopsys.MALForAndroid.DELETE")) {
			Bundle b = intent.getExtras();
			long id = b.getLong("id", 0);
			db.execSQL("update animelist set dirty = 3 where id = " + String.valueOf(id));
			deleteDone();
			pushDirty(db);
		} else if (s.equals("com.riotopsys.MALForAndroid.UPDATE")) {
			update(db, intent.getExtras());
		} else if (s.equals("com.riotopsys.MALForAndroid.ADD")) {
			add(db, intent.getExtras());
		}
		db.close();
	}

	private void add(SQLiteDatabase db, Bundle extras) {
		// long id = extras.getLong("id", 0);

		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		String user = perfs.getString("userName", "");
		String api = perfs.getString("api", "");
		String pass = perfs.getString("passwd", "");

		String cred = Base64.encodeBytes((user + ":" + pass).getBytes());
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
			sb.append("anime_id=").append(extras.getString("id"));
			sb.append("&status=").append(extras.getString("status"));

			OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

			wr.write(sb.toString());
			wr.flush();
			wr.close();

			int fred = con.getResponseCode();
			if (fred == 200) {
				AnimeRecord ar = new AnimeRecord();
				ar.id = Integer.parseInt(extras.getString("id"));
				ar.imageUrl = "";
				ar.memberScore = "";
				ar.rank ="";
				ar.status="";
				ar.synopsis="";
				ar.title="";
				ar.type="";
				ar.watchedStatus=extras.getString("status");
								
				db.execSQL(ar.insertStatement());
				pullExtras( db, Long.parseLong(extras.getString("id"))); 
			}

		} catch (Exception e) {
			Log.e("MALManager", "add", e);
		}
	}

	private void update(SQLiteDatabase db, Bundle extras) {
		long id = extras.getLong("id", 0);
		StringBuffer sb = new StringBuffer("update animeList set ");
		if (extras.containsKey("status")) {
			sb.append(" watchedStatus = '").append(extras.getString("status"));
		}
		if (extras.containsKey("watched")) {
			sb.append(" watchedEpisodes = '").append(String.valueOf(extras.getInt("watched")));
		}
		if (extras.containsKey("score")) {
			sb.append(" score = '").append(String.valueOf(extras.getInt("score")));
		}
		sb.append("', dirty = 2 where id = ").append(String.valueOf(id));
		db.execSQL(sb.toString());
		fetchDone();
	}

	private void pushDirty(SQLiteDatabase db) {
		HttpURLConnection con;
		URL url;

		Cursor c = db.rawQuery("select * from animeList where dirty <> 0;", null);

		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		String user = perfs.getString("userName", "");
		String api = perfs.getString("api", "");
		String pass = perfs.getString("passwd", "");

		String cred = Base64.encodeBytes((user + ":" + pass).getBytes());

		StringBuffer sb = new StringBuffer();

		if (c.moveToFirst()) {
			while (!c.isAfterLast()) {

				long id = c.getLong(c.getColumnIndex("id"));

				switch (c.getInt(c.getColumnIndex("dirty"))) {
					case 0:
						Log.e("MALManage", "WTF i said no 0");
						break;
					case 1:
						// only seen during an update
						break;
					case 2:
						// just a change

						try {
							sb.delete(0, sb.length());
							url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(id));

							con = (HttpURLConnection) url.openConnection();
							con.setDoOutput(true);
							con.setReadTimeout(10000);
							con.setConnectTimeout(15000);
							con.setRequestMethod("PUT");
							// con.setRequestMethod("POST");
							con.setRequestProperty("Authorization", "Basic " + cred);

							// sb.append( "_method=PUT\n" );
							sb.append("status=").append(c.getString(c.getColumnIndex("watchedStatus")));
							sb.append("&").append("episodes=").append(String.valueOf(c.getInt(c.getColumnIndex("watchedEpisodes")))).append('\n');
							sb.append("&").append("score=").append(String.valueOf(c.getInt(c.getColumnIndex("score")))).append('\n');
							// sb.append("score=1\n");

							OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());

							wr.write(sb.toString());
							wr.flush();
							wr.close();

							int fred = con.getResponseCode();
							fred++;
							// if (con.getResponseCode() == 200) {
							// db.execSQL("delete from animeList where id = " +
							// String.valueOf(id));
							// Toast.makeText(this.getBaseContext(),
							// "Waffles!, " +
							// String.valueOf(),Toast.LENGTH_LONG).show();
							// }

						} catch (Exception e) {
							Log.e("MALManager", "pushDirty:update", e);
						}

						break;
					case 3:
						// delete
						try {

							url = new URL("http://" + api + "/animelist/anime/" + String.valueOf(id));

							con = (HttpURLConnection) url.openConnection();
							con.setReadTimeout(10000 /* milliseconds */);
							con.setConnectTimeout(15000 /* milliseconds */);
							con.setRequestMethod("DELETE");
							con.setRequestProperty("Authorization", "Basic " + cred);

							con.connect();

							if (con.getResponseCode() == 200) {
								db.execSQL("delete from animeList where id = " + String.valueOf(id));
							}

						} catch (Exception e) {
							Log.e("MALManager", "pushDirty:delete", e);
						}
						break;
				}
				c.moveToNext();
			}
		}
		c.close();
	}

	private void pullImage(SQLiteDatabase db, long id) {

		String s = "select imageUrl from animeList where id = " + String.valueOf(id);

		Cursor c = db.rawQuery(s, null);
		c.moveToFirst();

		try {// copypasta
			URL url = new URL(c.getString(0));
			URLConnection ucon = url.openConnection();

			// File file = new File("sdcard/com.riotopsys.MALForAndroid.images/"
			// + String.valueOf(id));

			File root = Environment.getExternalStorageDirectory();
			File file = new File(root, "Android/data/com.riotopsys.MALForAndroid/images/" + String.valueOf(id));
			// File file = new File( String.valueOf(id));
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
			fetchDone();
		} catch (Exception e) {
			Log.e("MALManager", "Failed on img", e);
		}
		c.close();
	}

	private void fetchDone() {
		Intent i = new Intent("com.riotopsys.MALForAndroid.FETCH_COMPLETE");
		sendBroadcast(i);
	}

	private void deleteDone() {
		Intent i = new Intent("com.riotopsys.MALForAndroid.DELETE_COMPLETE");
		sendBroadcast(i);
	}

	private void pullExtras(SQLiteDatabase db, long id) {
		try {
			SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
			// String user = perfs.getString("userName", "");
			String api = perfs.getString("api", "");

			if (!api.equals("")) {

				URL url = new URL("http://" + api + "/anime/" + String.valueOf(id) + "?format=xml");
				InputSource in = new InputSource(new InputStreamReader(url.openStream()));

				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser parser = factory.newSAXParser();
				MALHandler handeler = new MALHandler(db);
				parser.parse(in, handeler);

				fetchDone();
			} else {
				Log.e("MALManager", "user not configured");
			}

		} catch (Exception e) {
			Log.e("MALManager", "Failed to pull", e);
		}
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

			Log.i("MALManager", "TIMER SET");
		}
	}

	private void pullList(SQLiteDatabase db) {

		NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(this, main.class);

		PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

		Notification notification = new Notification(R.drawable.icon, "Synchonizing", System.currentTimeMillis());
		notification.setLatestEventInfo(this, "MAL for Android", "Pulling anime list from MAL", pi);

		notification.flags |= Notification.FLAG_NO_CLEAR;
		mManager.notify(0, notification);

		try {
			SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
			String user = perfs.getString("userName", "");
			String api = perfs.getString("api", "");

			if (!api.equals("") && !user.equals("")) {

				db.execSQL(getString(R.string.dirty));// all is dirt

				URL url = new URL("http://" + api + "/animelist/" + user + "?format=xml");
				InputSource in = new InputSource(new InputStreamReader(url.openStream()));

				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser parser = factory.newSAXParser();
				MALHandler handeler = new MALHandler(db);
				parser.parse(in, handeler);

				db.execSQL(getString(R.string.clean));// remove the unclean ones

				fetchDone();
			} else {
				Log.e("MALManager", "user not configured");
			}

		} catch (Exception e) {
			Log.e("MALManager", "Failed to pull", e);
		}

		mManager.cancelAll();
	}
}
