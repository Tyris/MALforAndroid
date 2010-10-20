package com.riotopsys.MALforAndroid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Search extends Activity {

	private ListView lv;
	private EditText text;
	private Button search;
	
	private boolean animeMode; 

	ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
	private SimpleAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		lv = (ListView) findViewById(R.id.searchLv);
		text = (EditText) findViewById(R.id.textValue);
		search = (Button) findViewById(R.id.search);

		adapter = new SimpleAdapter(this, list, R.layout.search_item, new String[] { "title", "type", "synopsis", "episodes" }, new int[] {
				R.id.searchItemTitle, R.id.searchItemType, R.id.searchItemSynopsys, R.id.searchItemEpisodes });
		lv.setAdapter(adapter);

		registerForContextMenu(lv);
		
		lv.setOnItemClickListener( new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
				v.showContextMenu();
			}
		});

		SearchListener sl = new SearchListener();
		search.setOnClickListener(sl);
		
		Bundle b = getIntent().getExtras();
		animeMode = b.getBoolean("mode");
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		if ( animeMode ){
			inflater.inflate(R.menu.search_item, menu);
		} else {
			inflater.inflate(R.menu.search_item_manga, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		Intent i = new Intent(this, MALManager.class);
		i.setAction(MALManager.ADD);
		Bundle b = new Bundle();

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		MALRecord ar;
		if ( animeMode ){
			ar = new AnimeRecord();
		} else {
			ar = new MangaRecord();
		}
		ar.id = Long.parseLong(list.get(info.position).get("id"));

		switch (item.getItemId()) {
			case R.id.addCompleted:
				// b.putString("status", "completed");
				ar.watchedStatus = "completed";
				break;
			case R.id.addReading:
				// b.putString("status", "completed");
				ar.watchedStatus = "reading";
				break;
			case R.id.addDropped:
				// b.putString("status", "dropped");
				ar.watchedStatus = "dropped";
				break;
			case R.id.addOnHold:
				// b.putString("status", "on-hold");
				ar.watchedStatus = "on-hold";
				break;
			case R.id.addPlantoWatch:
				// b.putString("status", "plan to watch");
				ar.watchedStatus = "plan to watch";
				break;
			case R.id.addPlantoRead:
				// b.putString("status", "plan to watch");
				ar.watchedStatus = "plan to read";
				break;
			case R.id.addWatching:
				// b.putString("status", "watching");
				ar.watchedStatus = "watching";
				break;
		}

		b.putSerializable("media", ar);

		i.putExtras(b);
		startService(i);
		finish();
		return true;
	}

	private void runSearch() {
		try {
			list.clear();
			adapter.notifyDataSetChanged();

			SharedPreferences perfs = PreferenceManager.getDefaultSharedPreferences(this);
			String api = perfs.getString("api", "");
			
			URL url;
			if ( animeMode ){
				url = new URL("http://" + api + "/anime/search?q=" + URLEncoder.encode(text.getText().toString().trim(), "UTF-8"));
			} else {
				url = new URL("http://" + api + "/manga/search?q=" + URLEncoder.encode(text.getText().toString().trim(), "UTF-8"));
			}
			
			BufferedReader rd = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()), 512);
			String line;
			StringBuffer sb = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();

			JSONArray array = new JSONArray(sb.toString());
			for (int c = 0; c < array.length(); c++) {
				JSONObject jo = array.getJSONObject(c);
				HashMap<String, String> item = new HashMap<String, String>();

				item.put("id", jo.getString("id"));
				item.put("title", jo.getString("title"));
				item.put("type", jo.getString("type"));
				item.put("synopsis", jo.getString("synopsis"));
				if ( animeMode ){
					item.put("episodes", "Episodes: " + jo.getString("episodes"));
				} else {
					item.put("episodes", "Chapters: " + jo.getString("chapters"));
				}

				list.add(item);

			}

			adapter.notifyDataSetChanged();

		} catch (Exception e) {
			Toast.makeText(this, R.string.connectError, Toast.LENGTH_SHORT).show();
			Log.i("Search", "SearchListener", e);
		}
	}

	private class SearchListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			runSearch();
		}

	}

}
