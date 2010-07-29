package com.riotopsys.MALforAndroid;

import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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

		Log.i("MALManager", s);

		if (s.equals(Intent.ACTION_SEND)) {
			// send dirty to MAL
		} else if (s.equals(Intent.ACTION_SYNC)) {
			// get list from MAL
			Log.i("MALManager", "syncing");
			pullList(db);
			schedule();
		} else if (s.equals("SCHEDULE")) {
			schedule();
		}
		db.close();
	}
	
	private void schedule(){
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

				Intent i = new Intent("com.riotopsys.MALForAndroid.SYNC_COMPLETE");
				sendBroadcast(i);
			} else {
				Log.e("MALManager", "user not configured");
			}

		} catch (Exception e) {
			Log.e("MALManager", "Failed to pull: " + e.toString());
			Log.e("MALManager", Log.getStackTraceString(e));
		}
	}
}
