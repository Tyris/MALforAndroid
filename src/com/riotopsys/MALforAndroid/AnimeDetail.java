package com.riotopsys.MALforAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
	private TextView volumes;

	private IntentFilter intentFilter;

	// private long id;
	private MALRecord ar;
	private Reciever rec;
	
	private IntegerPicker ipWatched;
	private IntegerPicker ipScore;
	private IntegerPicker ipVolumes;

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

		volumes = (TextView) this.findViewById(R.id.detailVolumes);

		Bundle b = getIntent().getExtras();

		ar = (MALRecord) b.getSerializable("media");
		
		ipWatched = new IntegerPicker(this);
		ipWatched.setOnDismissListener(new WatchDismissed());
		ipVolumes = new IntegerPicker(this);
		ipVolumes.setOnDismissListener(new VolumesDismissed());

		ipScore = new IntegerPicker(this);
		ipScore.setTitle("Set Score");
		ipScore.setLimits(0, 10);
		ipScore.setOnDismissListener(new ScoreDismissed());
		
		if ( ar instanceof AnimeRecord ) {
			ipWatched.setTitle("Episodes Watched");			
		} else {
			ipWatched.setTitle("Chapter Read");			
		}

		display();

	}

	private void display() {

		Resources res = getResources();

		File root = Environment.getExternalStorageDirectory();
		File file;
		title.setText(ar.title);
		if (ar instanceof AnimeRecord) {
			progress.setText(String.valueOf(((AnimeRecord) ar).watchedEpisodes) + " of " + String.valueOf(((AnimeRecord) ar).episodes));
			file = new File(root, res.getString(R.string.imagePathAnime) + String.valueOf(ar.id));
			volumes.setVisibility(View.GONE);
		} else {
			progress.setText("Chap:" + String.valueOf(((MangaRecord) ar).chaptersRead) + " of " + String.valueOf(((MangaRecord) ar).chapters));
			volumes.setText("Vol:" + String.valueOf(((MangaRecord) ar).volumesRead) + " of " + String.valueOf(((MangaRecord) ar).volumes));
			file = new File(root, res.getString(R.string.imagePathManga) + String.valueOf(ar.id));
			volumes.setVisibility(View.VISIBLE);
		}
		score.setText(res.getString(R.string.detailScorePrefix) + String.valueOf(ar.score));
		status.setText(ar.status);
		type.setText(ar.type);
		watchedStatus.setText(ar.watchedStatus);

		if (ar.memberScore != null ) {
			memberScore.setText(res.getString(R.string.detailMemberScorePrefix) + ar.memberScore);
			rank.setText(res.getString(R.string.detailRankPrefix) + ar.rank);
			synopsis.setText(ar.synopsis);
			
			Intent i = new Intent(this, MALManager.class);
			i.setAction(MALManager.IMAGE);
			Bundle b = new Bundle();
			b.putSerializable("media", ar);
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
			
		} else {
			Intent i = new Intent(this, MALManager.class);
			i.setAction(MALManager.PULL);
			Bundle b = new Bundle();
			b.putSerializable("media", ar);
			i.putExtras(b);
			startService(i);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if ( ar instanceof AnimeRecord ){
			inflater.inflate(R.menu.detail, menu);
		} else {
			inflater.inflate(R.menu.detail_manga, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		boolean postIntent = false;
		int totalEp;
		int completed;
		
		Intent i;
		Bundle b;

		switch (itemId) {
			case R.id.detailMenuDelete:
				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("media", ar);
				i.putExtras(b);
				i.setAction(MALManager.REMOVE);
				startService(i);

				Toast.makeText(this, R.string.deleting, Toast.LENGTH_SHORT).show();
				finish();
				break;
			case R.id.detailMenuSync:
				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("media", ar);
				i.putExtras(b);
				i.setAction(MALManager.PULL);
				startService(i);

				i = new Intent(this, MALManager.class);
				b = new Bundle();
				b.putSerializable("media", ar);
				i.putExtras(b);
				i.setAction(MALManager.IMAGE);
				startService(i);
				break;
			case R.id.itemStatusCompleted:
				ar.watchedStatus = "completed";
				postIntent = true;
				break;
			case R.id.itemStatusReading:
				ar.watchedStatus = "reading";
				postIntent = true;
				break;
			case R.id.itemStatusDropped:
				ar.watchedStatus = "dropped";
				postIntent = true;
				break;
			case R.id.itemStatusOnHold:
				ar.watchedStatus = "on-hold";
				postIntent = true;
				break;
			case R.id.itemStatusPlantoWatch:
				ar.watchedStatus = "plan to watch";
				postIntent = true;
				break;
			case R.id.itemStatusPlantoRead:
				ar.watchedStatus = "plan to read";
				postIntent = true;
				break;
			case R.id.itemStatusWatching:
				ar.watchedStatus = "watching";
				postIntent = true;
				break;
			case R.id.setWatched:
				if (ar instanceof AnimeRecord) {
					totalEp = ((AnimeRecord) ar).episodes;
					completed = ((AnimeRecord) ar).watchedEpisodes;
				} else {
					totalEp = ((MangaRecord) ar).chapters;
					completed = ((MangaRecord) ar).chaptersRead;
				}

				if (totalEp == 0) {
					totalEp = Integer.MAX_VALUE;
				}
				ipWatched.setLimits(0, totalEp);
				ipWatched.setCurrent(completed);
				ipWatched.show();

				break;
			case R.id.setVolumeRead:

				totalEp = ((MangaRecord) ar).volumes;
				completed = ((MangaRecord) ar).volumesRead;

				if (totalEp == 0) {
					totalEp = Integer.MAX_VALUE;
				}
				ipVolumes.setLimits(0, totalEp);
				ipVolumes.setCurrent(completed);
				ipVolumes.show();

				break;
			case R.id.setScore:

				ipScore.setLimits(0, 10);
				ipScore.setCurrent(ar.score);
				ipScore.show();

				break;
				
		}
		
		if (postIntent) {
			i = new Intent(this, MALManager.class);
			i.setAction(MALManager.CHANGE);
			b = new Bundle();
			b.putSerializable("media", ar);
			i.putExtras(b);
			startService(i);
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
			if ( ar instanceof AnimeRecord ){
				ar = MALManager.getAnime(ar.id, getBaseContext());
			} else {
				ar = MALManager.getManga(ar.id, getBaseContext());
			}
			display();
		}

	}
	
	private class WatchDismissed implements OnDismissListener {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if (!ipWatched.wasCanceled()) {

				Intent i = new Intent(getBaseContext(), MALManager.class);
				i.setAction(MALManager.CHANGE);
				Bundle b = new Bundle();
				if (ar instanceof AnimeRecord) {
					((AnimeRecord) ar).watchedEpisodes = ipWatched.getCurrent();
				} else {
					((MangaRecord) ar).chaptersRead = ipWatched.getCurrent();
				}
				b.putSerializable("media", ar);

				i.putExtras(b);
				startService(i);

			}
		}
	}

	private class VolumesDismissed implements OnDismissListener {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if (!ipVolumes.wasCanceled()) {

				Intent i = new Intent(getBaseContext(), MALManager.class);
				i.setAction(MALManager.CHANGE);
				Bundle b = new Bundle();

				((MangaRecord) ar).volumesRead = ipVolumes.getCurrent();
				b.putSerializable("media", ar);

				i.putExtras(b);
				startService(i);

			}
		}
	}

	private class ScoreDismissed implements OnDismissListener {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if (!ipScore.wasCanceled()) {

				Intent i = new Intent(getBaseContext(), MALManager.class);
				i.setAction(MALManager.CHANGE);
				Bundle b = new Bundle();
				ar.score = ipScore.getCurrent();
				b.putSerializable("media", ar);

				i.putExtras(b);
				startService(i);

			}
		}
	}	
}
