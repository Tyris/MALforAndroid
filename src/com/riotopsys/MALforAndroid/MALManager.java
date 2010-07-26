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
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
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
		} else if (s.equals("SCHEDULE")) {
			// Intent i = new Intent(this, MALManager.class);
			// i.setAction(Intent.ACTION_SYNC);
			Intent i = new Intent(this, MALManager.class);
			i.setAction(Intent.ACTION_SYNC);
			PendingIntent mAlarmSender = PendingIntent.getService(this, 0, i, 0);

			//long firstTime = SystemClock.elapsedRealtime()+10*1000;
			long firstTime = SystemClock.elapsedRealtime()+AlarmManager.INTERVAL_HOUR;

			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			//am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 60 * 1000, mAlarmSender);
			am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, AlarmManager.INTERVAL_HOUR, mAlarmSender);

			Log.i("MALManager", "TIMER SET");
		}
		db.close();
	}

	private void pullList(SQLiteDatabase db) {
		try {
			URL url = new URL("http://mal-api.com/animelist/riotopsys?format=xml");
			InputSource in = new InputSource(new InputStreamReader(url.openStream()));

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			MALHandler handeler = new MALHandler(db);
			parser.parse(in, handeler);

			Intent i = new Intent("com.riotopsys.MALForAndroid.SYNC_COMPLETE");
			sendBroadcast(i);

		} catch (Exception e) {
			Log.e("MALManager", "Failed to pull: " + e.toString());
			Log.e("MALManager", Log.getStackTraceString(e));
		}
	}
}
