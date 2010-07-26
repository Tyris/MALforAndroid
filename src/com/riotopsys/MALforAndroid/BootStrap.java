package com.riotopsys.MALforAndroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootStrap extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("BootStrap", "strapped");
		Intent i = new Intent(context, MALManager.class);
		i.setAction("SCHEDULE");
		context.startService(i);
	}

}
