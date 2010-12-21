package com.riotopsys.MALforAndroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity {

	private final static String LOG_NAME = "MAL Main";

	private TextView title;
	private ListView lv;
	private Spinner spinner;
	private SQLiteDatabase db;
	private SimpleCursorAdapter adapter;
	private String sort;
	private int lastChoice;
	private Reciever rec;
	private IntentFilter intentFilter;

	private IntegerPicker ipWatched;
	private IntegerPicker ipScore;
	private IntegerPicker ipVolumes;
	private MALRecord longClickRecord;


	private boolean animeMode;

	// private MALAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.xml.preferances, false);
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());		

		title = (TextView) findViewById(R.id.mainTitle);
		lv = (ListView) findViewById(R.id.lv);
		
		ipWatched = new IntegerPicker(this);
		ipWatched.setOnDismissListener(new WatchDismissed());
		ipVolumes = new IntegerPicker(this);
		ipVolumes.setOnDismissListener(new VolumesDismissed());

		ipScore = new IntegerPicker(this);
		ipScore.setTitle("Set Score");
		ipScore.setLimits(0, 10);
		ipScore.setOnDismissListener(new ScoreDismissed());
		
		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setOnItemSelectedListener(new FilterSelected());

		if (savedInstanceState != null) {
			animeMode = savedInstanceState.getBoolean("mode", true);
			initList();
			sort = savedInstanceState.getString("sort");
			spinner.setSelection(savedInstanceState.getInt("filter"));
			lastChoice = Integer.valueOf(savedInstanceState.getInt("filter"));
		} else {
			animeMode = perfs.getString("mode", getString(R.string.prefModeDefault)).equals(getString(R.string.prefModeDefault));
			initList();
			sort = perfs.getString("sort", getString(R.string.titleSort));
			Log.i(LOG_NAME,perfs.getString("filter", "0"));
			spinner.setSelection(Integer.valueOf(perfs.getString("filter", "0")));
			lastChoice = Integer.valueOf(perfs.getString("filter", "0"));
		}


		adapter = new SimpleCursorAdapter(this, R.layout.mal_item, null, new String[] { "title", "watchedEpisodes", "episodes", "score" }, new int[] {
				R.id.title, R.id.complete, R.id.total, R.id.score });
		lv.setAdapter(adapter);

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		db = openHelper.getReadableDatabase();

		registerForContextMenu(lv);

		//spinner.setOnItemSelectedListener(new FilterSelected());
		lv.setOnItemClickListener(new AnimeSelected(getBaseContext()));

		intentFilter = new IntentFilter(MALManager.RELOAD);

		rec = new Reciever();

		registerReceiver(rec, intentFilter);
		
		setFilter(lastChoice);

		if (perfs.getString("userName", "").equals("") || perfs.getString("api", "").equals("")) {
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			if (perfs.getString("userName", "").equals("")) {
				Toast.makeText(this, R.string.accountSetup, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, R.string.apiSetup, Toast.LENGTH_LONG).show();
			}
		}
	}

	private void initList() {
		ArrayAdapter<CharSequence> spinnnerAdapter;
		if (animeMode) {
			spinnnerAdapter = ArrayAdapter.createFromResource(this, R.array.filterArray, android.R.layout.simple_spinner_item);
			// adapter = new SimpleCursorAdapter(this, R.layout.mal_item, null,
			// new String[] { "title", "watchedEpisodes", "episodes", "score" },
			// new int[] {R.id.title, R.id.complete, R.id.total, R.id.score });
			ipWatched.setTitle("Episodes Watched");
			title.setText("Anime List");
		} else {
			spinnnerAdapter = ArrayAdapter.createFromResource(this, R.array.filterArrayManga, android.R.layout.simple_spinner_item);
			// adapter = new SimpleCursorAdapter(this, R.layout.mal_item, null,
			// new String[] { "title", "watchedEpisodes", "episodes", "score" },
			// new int[] {R.id.title, R.id.complete, R.id.total, R.id.score });
			ipWatched.setTitle("Chapter Read");
			title.setText("Manga List");
		}
		spinnnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnnerAdapter);
		// lv.setAdapter(adapter);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();

		if (animeMode) {
			inflater.inflate(R.menu.item_menu, menu);
			longClickRecord = MALManager.getAnime(((AdapterContextMenuInfo) menuInfo).id, this.getBaseContext());
		} else {
			inflater.inflate(R.menu.item_menu_manga, menu);
			longClickRecord = MALManager.getManga(((AdapterContextMenuInfo) menuInfo).id, this.getBaseContext());
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean postIntent = false;
		int totalEp;
		int completed;

		switch (item.getItemId()) {
			case R.id.itemStatusCompleted:
				longClickRecord.watchedStatus = "completed";
				postIntent = true;
				break;
			case R.id.itemStatusReading:
				longClickRecord.watchedStatus = "reading";
				postIntent = true;
				break;
			case R.id.itemStatusDropped:
				longClickRecord.watchedStatus = "dropped";
				postIntent = true;
				break;
			case R.id.itemStatusOnHold:
				longClickRecord.watchedStatus = "on-hold";
				postIntent = true;
				break;
			case R.id.itemStatusPlantoWatch:
				longClickRecord.watchedStatus = "plan to watch";
				postIntent = true;
				break;
			case R.id.itemStatusPlantoRead:
				longClickRecord.watchedStatus = "plan to read";
				postIntent = true;
				break;
			case R.id.itemStatusWatching:
				longClickRecord.watchedStatus = "watching";
				postIntent = true;
				break;
			case R.id.setWatched:
				if (longClickRecord instanceof AnimeRecord) {
					totalEp = ((AnimeRecord) longClickRecord).episodes;
					completed = ((AnimeRecord) longClickRecord).watchedEpisodes;
				} else {
					totalEp = ((MangaRecord) longClickRecord).chapters;
					completed = ((MangaRecord) longClickRecord).chaptersRead;
				}

				if (totalEp == 0) {
					totalEp = Integer.MAX_VALUE;
				}
				ipWatched.setLimits(0, totalEp);
				ipWatched.setCurrent(completed);
				ipWatched.show();

				break;
			case R.id.setVolumeRead:

				totalEp = ((MangaRecord) longClickRecord).volumes;
				completed = ((MangaRecord) longClickRecord).volumesRead;

				if (totalEp == 0) {
					totalEp = Integer.MAX_VALUE;
				}
				ipVolumes.setLimits(0, totalEp);
				ipVolumes.setCurrent(completed);
				ipVolumes.show();

				break;
			case R.id.setScore:

				ipScore.setLimits(0, 10);
				ipScore.setCurrent(longClickRecord.score);
				ipScore.show();

				break;
			case R.id.menuItemDelete:
				Intent i = new Intent(this, MALManager.class);
				i.setAction(MALManager.REMOVE);
				Bundle b = new Bundle();
				b.putSerializable("media", longClickRecord);
				i.putExtras(b);
				startService(i);
				break;
		}

		if (postIntent) {
			Intent i = new Intent(this, MALManager.class);
			i.setAction(MALManager.CHANGE);
			Bundle b = new Bundle();
			b.putSerializable("media", longClickRecord);
			i.putExtras(b);
			startService(i);
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public void onPause() {
		unregisterReceiver(rec);
		// adapter.getCursor().close();
		super.onPause();
	}

	@Override
	public void onResume() {
		setFilter(lastChoice);
		registerReceiver(rec, intentFilter);
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Intent i;
		switch (id) {
			case R.id.sortTitle:
				sort = getString(R.string.titleSort);
				setFilter(lastChoice);
				break;
			case R.id.sortScore:
				sort = getString(R.string.scoreSort);
				setFilter(lastChoice);
				break;
			case R.id.sortComplete:
				sort = getString(R.string.completeSort);
				setFilter(lastChoice);
				break;
			case R.id.menuSync:
				i = new Intent(this, MALManager.class);
				i.setAction(MALManager.SYNC);
				startService(i);
				break;
			case R.id.menuSettings:
				i = new Intent(this, Preferences.class);
				startActivity(i);
				break;
			case R.id.menuSwitch:
				animeMode = (!animeMode);
				initList();
				spinner.setSelection(lastChoice);
				setFilter(lastChoice);
				break;
			case R.id.menuAdd:
				i = new Intent(this, Search.class);
				Bundle b = new Bundle();
				b.putBoolean("mode", animeMode);
				i.putExtras(b);				
				startActivity(i);
				break;
		}
		return true;
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("sort", sort);
		savedInstanceState.putInt("filter", lastChoice);
		savedInstanceState.putBoolean("mode", animeMode);

		super.onSaveInstanceState(savedInstanceState);
	}

	private void setFilter(int choice) {
		lastChoice = choice;
		String query;
		if (animeMode) {
			query = getString(R.string.cursorSelect) + getResources().getStringArray(R.array.filterWhere)[choice] + " and dirty <> 3 " + sort;
		} else {
			query = getString(R.string.cursorSelectManga) + getResources().getStringArray(R.array.filterWhereManga)[choice] + " and dirty <> 3 " + sort;
		}

		try {
			// adapter.getCursor().close();
			Cursor c = db.rawQuery(query, null);
			adapter.changeCursor(c);
		} catch (Exception e) {
			Log.e("main", "Query failure", e);
			Log.e(query, Log.getStackTraceString(e));
		}

	}

	private class FilterSelected implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
			setFilter(position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	private class AnimeSelected implements OnItemClickListener {

		private Context context;

		public AnimeSelected(Context context) {
			this.context = context;
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			// Toast.makeText(context, "item: "+ String.valueOf(id),
			// Toast.LENGTH_LONG).show();
			Intent i = new Intent(context, AnimeDetail.class);
			Bundle b = new Bundle();
			if (animeMode) {
				b.putSerializable("media", MALManager.getAnime(id, getBaseContext()));
			} else {
				b.putSerializable("media", MALManager.getManga(id, getBaseContext()));
			}
			i.putExtras(b);
			startActivity(i);
		}

	}

	private class Reciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Log.i(LOG_NAME, arg1.getAction());
			setFilter(lastChoice);
		}

	}

	private class WatchDismissed implements OnDismissListener {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if (!ipWatched.wasCanceled()) {

				Intent i = new Intent(getBaseContext(), MALManager.class);
				i.setAction(MALManager.CHANGE);
				Bundle b = new Bundle();
				if (longClickRecord instanceof AnimeRecord) {
					((AnimeRecord) longClickRecord).watchedEpisodes = ipWatched.getCurrent();
				} else {
					((MangaRecord) longClickRecord).chaptersRead = ipWatched.getCurrent();
				}
				b.putSerializable("media", longClickRecord);

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

				((MangaRecord) longClickRecord).volumesRead = ipVolumes.getCurrent();
				b.putSerializable("media", longClickRecord);

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
				longClickRecord.score = ipScore.getCurrent();
				b.putSerializable("media", longClickRecord);

				i.putExtras(b);
				startService(i);

			}
		}
	}	
}
