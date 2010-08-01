package com.riotopsys.MALforAndroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class AnimeDetail extends Activity {

	private TextView title;
	private TextView progress;
	private TextView score;
	private TextView status;
	private TextView type;
	private TextView watchedStatus;
	@SuppressWarnings("unused")
	private ImageView image;
	private TextView memberScore;
	private TextView rank;
	private TextView synopsis;
	private IntentFilter intentFilter;
	
	private long id;
	private Reciever rec;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);
		
		intentFilter = new IntentFilter("com.riotopsys.MALForAndroid.FETCH_COMPLETE");
		
		rec = new Reciever();
		
		registerReceiver(rec, intentFilter);

		title = (TextView) this.findViewById(R.id.detailTitle);
		progress = (TextView) this.findViewById(R.id.detailProgress);
		score = (TextView) this.findViewById(R.id.detailScore);
		status = (TextView) this.findViewById(R.id.detailStatus);
		type = (TextView) this.findViewById(R.id.detailType);
		watchedStatus = (TextView) this.findViewById(R.id.detailWatchedStatus);

		image = (ImageView) this.findViewById(R.id.detailImage);
		memberScore = (TextView) this.findViewById(R.id.detailMemberScore);
		rank = (TextView) this.findViewById(R.id.detailRank);
		synopsis = (TextView) this.findViewById(R.id.detailSynopsis);
		
		Bundle b = getIntent().getExtras();

		id = b.getLong("id", 0);

		display();
		
		/*MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		SQLiteDatabase db = openHelper.getReadableDatabase();
		
		String s = "select * from animeList where id = " + String.valueOf(id);

		Cursor c = db.rawQuery(s, null);

		if (c != null) {
			if (c.moveToFirst()) {

				title.setText(c.getString(1));
				progress.setText(c.getString(6) + " of " + c.getString(4));
				score.setText("Score: " + c.getString(7));
				status.setText(c.getString(5));
				type.setText(c.getString(2));
				watchedStatus.setText(c.getString(8));

				if (!c.isNull(10)) {
					memberScore.setText("Member Score: "+c.getString(10));
					rank.setText("Rank: " + c.getString(11));
					synopsis.setText(new String(c.getBlob(12)));

				} else {
					memberScore.setText("");
					rank.setText("");
					synopsis.setText("");

					Intent i = new Intent(this, MALManager.class);
					i.setAction("com.riotopsys.MALForAndroid.FETCH_EXTRAS");
					// b = new Bundle();
					// b.putLong("id", id);
					i.putExtras(b);
					startService(i);
				}

			}
			c.close();

		}

		db.close();*/
	}
	
	private void display(){
		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		SQLiteDatabase db = openHelper.getReadableDatabase();
		
		String s = "select * from animeList where id = " + String.valueOf(id);

		Cursor c = db.rawQuery(s, null);

		if (c != null) {
			if (c.moveToFirst()) {

				title.setText(c.getString(1));
				progress.setText(c.getString(6) + " of " + c.getString(4));
				score.setText("Score: " + c.getString(7));
				status.setText(c.getString(5));
				type.setText(c.getString(2));
				watchedStatus.setText(c.getString(8));

				if (!c.isNull(10)) {
					memberScore.setText("Member Score: "+c.getString(10));
					rank.setText("Rank: " + c.getString(11));
					synopsis.setText(new String(c.getBlob(12)));

				} else {
					memberScore.setText("");
					rank.setText("");
					synopsis.setText("");

					Intent i = new Intent(this, MALManager.class);
					i.setAction("com.riotopsys.MALForAndroid.FETCH_EXTRAS");
					Bundle b = new Bundle();
					b.putLong("id", id);
					i.putExtras(b);
					startService(i);
				}

			}
			c.close();

		}

		db.close();
	}
	
	@Override 
	public void onPause(){
		unregisterReceiver( rec );
		super.onPause();
	}
	
	@Override 
	public void onResume(){
		registerReceiver( rec, intentFilter );
		super.onPause();
	}
	
	private class Reciever extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			display();
		}
		
	}

}
