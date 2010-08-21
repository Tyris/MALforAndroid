package com.riotopsys.MALforAndroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
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
import android.widget.Toast;

public class main extends Activity {

	private ListView lv;
	private SQLiteDatabase db;
	private SimpleCursorAdapter adapter;
	private String sort;
	private int lastChoice;
	private Reciever rec;
	private IntentFilter intentFilter;

	private IntegerPicker ipWatched;
	private IntegerPicker ipScore;
	private main self;
	private AnimeRecord LongClickRecord;

	// private MALAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		lv = (ListView) findViewById(R.id.lv);

		self = this;

		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> spinnnerAdapter = ArrayAdapter.createFromResource(this, R.array.filterArray, android.R.layout.simple_spinner_item);
		spinnnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnnerAdapter);

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		db = openHelper.getReadableDatabase();

		adapter = new SimpleCursorAdapter(this, R.layout.mal_item, null, new String[] { "title", "watchedEpisodes", "episodes", "score" }, new int[] {
				R.id.title, R.id.complete, R.id.total, R.id.score });
		lv.setAdapter(adapter);

		registerForContextMenu(lv);

		PreferenceManager.setDefaultValues(this, R.xml.preferances, false);
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
		perfs.registerOnSharedPreferenceChangeListener(new PerfChange());

		if (savedInstanceState != null) {
			sort = savedInstanceState.getString("sort");
			spinner.setSelection(savedInstanceState.getInt("filter"));
		} else {
			sort = perfs.getString("sort", getString(R.string.titleSort));
			spinner.setSelection(Integer.valueOf(perfs.getString("filter", "0")));
		}

		spinner.setOnItemSelectedListener(new FilterSelected());
		lv.setOnItemClickListener(new AnimeSelected(this.getBaseContext()));

		intentFilter = new IntentFilter(MALManager.RELOAD);

		rec = new Reciever();

		registerReceiver(rec, intentFilter);

		ipWatched = new IntegerPicker(this);
		ipWatched.setTitle("Episodes Watched");
		ipWatched.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				// Log.i("watched", String.valueOf(ipWatched.wasCanceled()) +
				// " " + String.valueOf(ipWatched.getCurrent()));
				if (!ipWatched.wasCanceled()) {

					Intent i = new Intent(self, MALManager.class);
					i.setAction(MALManager.CHANGE);
					Bundle b = new Bundle();
					LongClickRecord.watchedEpisodes = ipWatched.getCurrent();
					b.putSerializable("anime", LongClickRecord);

					i.putExtras(b);
					startService(i);

				}
			}
		});

		ipScore = new IntegerPicker(this);
		ipScore.setTitle("Set Score");
		ipScore.setLimits(0, 10);
		ipScore.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				// Log.i("watched", String.valueOf(ipScore.wasCanceled()) + " "
				// + String.valueOf(ipScore.getCurrent()));
				if (!ipScore.wasCanceled()) {

					Intent i = new Intent(self, MALManager.class);
					i.setAction(MALManager.CHANGE);
					Bundle b = new Bundle();
					LongClickRecord.score = ipScore.getCurrent();
					b.putSerializable("anime", LongClickRecord);

					i.putExtras(b);
					startService(i);
					
				}
			}
		});

		if (perfs.getString("userName", "").equals("") || perfs.getString("api", "").equals("")) {
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			if (perfs.getString("userName", "").equals("")) {
				Toast.makeText(this, "Please setup your account", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Please setup MAL API", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.item_menu, menu);

		LongClickRecord = MALManager.getAnime(((AdapterContextMenuInfo) menuInfo).id, this.getBaseContext());

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean postIntent = false;

		switch (item.getItemId()) {
			case R.id.itemStatusCompleted:
				LongClickRecord.status = "completed";
				postIntent = true;
				break;
			case R.id.itemStatusDropped:
				LongClickRecord.status = "dropped";
				postIntent = true;
				break;
			case R.id.itemStatusOnHold:
				LongClickRecord.status = "on-hold";
				postIntent = true;
				break;
			case R.id.itemStatusPlantoWatch:
				LongClickRecord.status = "plan to watch";
				postIntent = true;
				break;
			case R.id.itemStatusWatching:
				LongClickRecord.status = "watching";
				postIntent = true;
				break;
			case R.id.setWatched:

				int totalEp = LongClickRecord.episodes;
				if (totalEp == 0) {
					totalEp = Integer.MAX_VALUE;
				}
				ipWatched.setLimits(0, totalEp);
				ipWatched.setCurrent(LongClickRecord.watchedEpisodes);
				ipWatched.show();

				break;
			case R.id.setScore:

				ipScore.setLimits(0, 10);
				ipScore.setCurrent(LongClickRecord.score);
				ipScore.show();

				break;
		}

		if (postIntent) {
			Intent i = new Intent(this, MALManager.class);
			i.setAction(MALManager.CHANGE);
			Bundle b = new Bundle();
			b.putSerializable("anime", LongClickRecord);
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
			case R.id.menuAdd:
				i = new Intent(this, Search.class);
				startActivity(i);
				break;
		}
		return true;
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("sort", sort);
		savedInstanceState.putInt("filter", lastChoice);

		super.onSaveInstanceState(savedInstanceState);
	}

	private void setFilter(int choice) {
		lastChoice = choice;
		Resources res = this.getResources();
		String query = getString(R.string.cursorSelect) + res.getStringArray(R.array.filterWhere)[choice] + " and dirty <> 3 " + sort;

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
			b.putSerializable("anime", MALManager.getAnime(id, getBaseContext()));
			i.putExtras(b);
			startActivity(i);
		}

	}

	private class Reciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			setFilter(lastChoice);
		}

	}

	private class PerfChange implements OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
			if (key.equals("userName") || key.equals("passwd")) {
				Intent i = new Intent(self, MALManager.class);
				i.setAction(MALManager.SYNC);
				startService(i);
			}
		}
	}

}