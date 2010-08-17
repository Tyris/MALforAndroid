package com.riotopsys.MALforAndroid;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

		adapter = new SimpleAdapter(this, list, R.layout.search_item, new String[] { "title", "type", "synopsis" }, new int[] { R.id.searchItemTitle,
				R.id.searchItemType, R.id.searchItemSynopsys });
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
		i.setAction("com.riotopsys.MALForAndroid.ADD");
		Bundle b = new Bundle();
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    String id = list.get( info.position ).get("id");
	    
	    b.putString("id", id);
		Log.d("stuff", String.valueOf(item.getItemId()) );

		switch (item.getItemId()) {
			case R.id.addCompleted:
				b.putString("status", "completed");
				break;
			case R.id.addDropped:
				b.putString("status", "dropped");
				break;
			case R.id.addOnHold:
				b.putString("status", "on-hold");
				break;
			case R.id.addPlantoWatch:
				b.putString("status", "plan to watch");
				break;
			case R.id.addWatching:
				b.putString("status", "watching");
				break;
		}
		i.putExtras(b);
		startService(i);
		finish();
		return true;
	}
	
	
	
	private void runSearch(){
		try {
			list.clear();
			adapter.notifyDataSetChanged();
			URL url = new URL("http://mal-api.com/anime/search?format=xml&q=" + URLEncoder.encode(text.getText().toString().trim(),"UTF-8"));

			InputSource in = new InputSource(new InputStreamReader(url.openStream()));

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();

			parser.parse(in, new SearchHandeler());
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

	private class SearchHandeler extends DefaultHandler {
		StringBuffer sb;
		HashMap<String, String> item;

		SearchHandeler() {
			sb = new StringBuffer();
			item = new HashMap<String, String>();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			sb.append(new String(ch, start, length));
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			super.endElement(uri, localName, name);
			String s = sb.toString().trim();
			if (localName != null) {
				if (localName.equals("anime")) {

					list.add(item);
					adapter.notifyDataSetChanged();
					// notes.notifyDataSetChanged();
					item = null;
					item = new HashMap<String, String>();

				} else if (localName.equals("id")) {
					item.put("id", s);
				} else if (localName.equals("title")) {
					item.put("title", s);
				} else if (localName.equals("type")) {
					item.put("type", s);
				} else if (localName.equals("synopsis")) {
					item.put("synopsis", s);
				}

				sb.delete(0, sb.length());
			}
		}
	}
}
