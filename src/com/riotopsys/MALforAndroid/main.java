package com.riotopsys.MALforAndroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

	// private MALAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		lv = (ListView) findViewById(R.id.lv);

		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> spinnnerAdapter = ArrayAdapter.createFromResource(this, R.array.filterArray, android.R.layout.simple_spinner_item);
		spinnnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnnerAdapter);

		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		db = openHelper.getReadableDatabase();


		adapter = new SimpleCursorAdapter(this, R.layout.mal_item, null, new String[] { "title", "watchedEpisodes", "episodes", "score" }, new int[] {
				R.id.title, R.id.complete, R.id.total, R.id.score });
		lv.setAdapter(adapter);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferances, false);
		SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);

		if (savedInstanceState != null) {
			sort = savedInstanceState.getString("sort");
			spinner.setSelection(savedInstanceState.getInt("filter"));
		} else {
			sort =perfs.getString("sort", getString(R.string.titleSort)); 
			spinner.setSelection(Integer.valueOf(perfs.getString("filter", "0")));
		}

		spinner.setOnItemSelectedListener(new FilterSelected());
		lv.setOnItemClickListener(new AnimeSelected(this.getBaseContext()));
		
		intentFilter = new IntentFilter("com.riotopsys.MALForAndroid.SYNC_COMPLETE");
		
		rec = new Reciever();
		
		registerReceiver(rec, intentFilter);
		
		if ( perfs.getString("userName", "").equals("") || perfs.getString("api", "").equals("")  ){
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			if ( perfs.getString("userName", "").equals("") ){
				Toast.makeText(this, "Please setup your account", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Please setup MAL API", Toast.LENGTH_LONG).show();
			}			
		} 			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
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
				i.setAction(Intent.ACTION_SYNC);
				startService(i);
				break;
			case R.id.menuSettings:
				i = new Intent(this, Preferences.class);
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
		String query = getString(R.string.cursorSelect) + res.getStringArray(R.array.filterWhere)[choice] + sort;

		try {
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
			//Toast.makeText(context, "item: "+ String.valueOf(id), Toast.LENGTH_LONG).show();			
			Intent i = new Intent( context, AnimeDetail.class );
			Bundle b = new Bundle();
			b.putLong("id", id);
			i.putExtras(b);
			startActivity(i);						
		}

	}
	
	private class Reciever extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			setFilter(lastChoice);				
		}
		
	}

}