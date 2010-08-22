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
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class Search extends Activity {

	private ListView lv;
	private EditText text;
	private Button search;

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

		SearchListener sl = new SearchListener();
		search.setOnClickListener(sl);
		text.setOnClickListener(sl);

		text.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					runSearch();
					return true;
				}
				return false;
			}
		});

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_item, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		Intent i = new Intent(this, MALManager.class);
		i.setAction(MALManager.ADD);
		Bundle b = new Bundle();

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		AnimeRecord ar = new AnimeRecord();
		ar.id = Long.parseLong(list.get(info.position).get("id"));

		Log.d("stuff", String.valueOf(item.getItemId()));

		switch (item.getItemId()) {
			case R.id.addCompleted:
				// b.putString("status", "completed");
				ar.watchedStatus = "completed";
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
			case R.id.addWatching:
				// b.putString("status", "watching");
				ar.watchedStatus = "watching";
				break;
		}

		b.putSerializable("anime", ar);

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

			URL url = new URL("http://" + api + "/anime/search?q=" + URLEncoder.encode(text.getText().toString().trim(), "UTF-8"));
			
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
				item.put("episodes", "Episodes: " + jo.getString("episodes"));

				list.add(item);

			}

			adapter.notifyDataSetChanged();

		} catch (Exception e) {
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
