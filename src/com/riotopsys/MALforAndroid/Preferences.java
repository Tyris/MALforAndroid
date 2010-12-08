package com.riotopsys.MALforAndroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences extends PreferenceActivity {

	private final static String LOG_NAME = "Preferences.java";

	// private PerfChange pfChang;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferances);
		PreferenceManager.setDefaultValues(this, R.xml.preferances, false);
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		PerfChange pfChang = new PerfChange();
		perfs.registerOnSharedPreferenceChangeListener(pfChang);
	}

	@SuppressWarnings("rawtypes")
	private class ImmediateActions extends AsyncTask {

		@Override
		protected Object doInBackground(Object... arg0) {
			Looper.prepare();
			String key = (String) arg0[0];
			if (key.equals("userName") || key.equals("passwd")) {
				if (MALManager.verifyCredentials(getBaseContext())) {					
					//Toast.makeText(getBaseContext(), R.string.firstSync, Toast.LENGTH_LONG * 2).show();
					Intent i = new Intent(getBaseContext(), MALManager.class);
					i.setAction(MALManager.SYNC);
					startService(i);
				} else {
					Log.i(LOG_NAME, "verifyCredentials: failed");
				}
			} else {
				Log.i(LOG_NAME, "Credentials changed");
			}
			if (key.equals("updateFreq")) {
				Log.i(LOG_NAME, "chg sync");
				Intent i = new Intent(getBaseContext(), MALManager.class);
				i.setAction(MALManager.SCHEDULE);
				getBaseContext().startService(i);
			}
			return null;
		}
	}

	private class PerfChange implements OnSharedPreferenceChangeListener {
		@SuppressWarnings("unchecked")
		@Override
		public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
			new ImmediateActions().execute(key);			
		}

	}

}
