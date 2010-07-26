package com.riotopsys.MALforAndroid;

import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

		if (s.equals(Intent.ACTION_SEND)) {
			// send dirty to MAL
		} else if (s.equals(Intent.ACTION_SYNC)) {
			// get list from MAL
			pullList(db);
		}
	}

	private void pullList(SQLiteDatabase db) {
		try {
			URL url = new URL("http://mal-api.com/animelist/riotopsys?format=xml");
			InputSource in = new InputSource(new InputStreamReader(url.openStream()));

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			MALHandler handeler = new MALHandler(db );
			parser.parse(in, handeler);
			
			Intent i = new Intent("com.riotopsys.MALForAndroid.SYNC_COMPLETE");
			sendBroadcast(i);
			
		} catch (Exception e) {
			Log.e("MALManager", "Failed to pull: "+e.toString());
			Log.e("MALManager", Log.getStackTraceString(e) );
		}
	}
}
