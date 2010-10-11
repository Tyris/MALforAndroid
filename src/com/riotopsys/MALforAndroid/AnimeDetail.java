package com.riotopsys.MALforAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AnimeDetail extends Activity {

	private TextView title;
	private TextView progress;
	private TextView score;
	private TextView status;
	private TextView type;
	private TextView watchedStatus;
	private ImageView image;
	private TextView memberScore;
	private TextView rank;
	private TextView synopsis;
	private IntentFilter intentFilter;

	// private long id;
	private AnimeRecord ar;
	private Reciever rec;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);

		intentFilter = new IntentFilter(MALManager.RELOAD);

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

		ar = (AnimeRecord) b.getSerializable("media");

		display();

	}

	private void display() {
		
		Resources res = getResources();

		title.setText(ar.title);
		progress.setText(String.valueOf(ar.watchedEpisodes) + " of " + String.valueOf(ar.episodes));
		score.setText( res.getString(R.string.detailScorePrefix) + String.valueOf(ar.score));
		status.setText(ar.status);
		type.setText(ar.type);
		watchedStatus.setText(ar.watchedStatus);

		memberScore.setText( res.getString(R.string.detailScorePrefix) + ar.memberScore);
		rank.setText( res.getString(R.string.detailRankPrefix) + ar.rank);
		synopsis.setText(ar.synopsis);

		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, res.getString(R.string.imagePathAnime) + String.valueOf(ar.id));
		Intent i = new Intent(this, MALManager.class);
		i.setAction(MALManager.IMAGE);
		Bundle b = new Bundle();
		b.putSerializable("anime", ar);
		i.putExtras(b);

		if (file.exists()) {

			try {
				FileInputStream fis = new FileInputStream(file);
				Bitmap bmImg = BitmapFactory.decodeStream(fis);
				if (bmImg != null) {
					image.setImageBitmap(bmImg);
				} else {
					startService(i);
				}
			} catch (FileNotFoundException e) {
				Log.e("AnimeDetail", "image error", e);
			}

		} else {
			startService(i);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		Intent i;
		Bundle b;
		
		switch (itemId) {
			case R.id.detailMenuDelete:
				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("anime", ar);
				i.putExtras(b);
				i.setAction(MALManager.REMOVE);
				startService(i);
		
				Toast.makeText(this, R.string.deleting, Toast.LENGTH_SHORT).show();
				finish();				
				break;
			case R.id.detailMenuSync:
				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("anime", ar);
				i.putExtras(b);
				i.setAction(MALManager.PULL);
				startService(i);
				
				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("anime", ar);
				i.putExtras(b);
				i.setAction(MALManager.IMAGE);
				startService(i);
				break;
		}
		return true;
	}

	@Override
	public void onPause() {
		unregisterReceiver(rec);
		super.onPause();
	}

	@Override
	public void onResume() {
		registerReceiver(rec, intentFilter);
		super.onPause();
	}

	private class Reciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			display();
		}

	}

}
