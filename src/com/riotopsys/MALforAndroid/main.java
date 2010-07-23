package com.riotopsys.MALforAndroid;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class main extends Activity {

	private TextView tv;
	private ListView lv;
	private MALAdapter adapter;
	private LinkedList<AnimeRecord> list;
	private SQLiteDatabase db;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		list = new LinkedList<AnimeRecord>();

		tv = (TextView) findViewById(R.id.tv);
		lv = (ListView) findViewById(R.id.lv);
		adapter = new MALAdapter(list);
		
		MALSqlHelper openHelper = new MALSqlHelper(this.getBaseContext());
		db = openHelper.getWritableDatabase();

		lv.setAdapter(adapter);
		try{
			db.execSQL(getString(R.string.anime_table_def));
		}catch (Exception e){
			tv.setText(e.toString());
		}

		tv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				URL url;
				try {
					url = new URL("http://mal-api.com/animelist/riotopsys?format=xml");
					new DownloadFilesTask().execute(url);
				} catch (MalformedURLException e) {
				}

			}
		});

	}

	private class DownloadFilesTask extends AsyncTask<URL, Object, LinkedList<AnimeRecord>> {

		protected LinkedList<AnimeRecord> doInBackground(URL... urls) {
			try {
				InputSource in = new InputSource(new InputStreamReader(urls[0].openStream()));

				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser parser = factory.newSAXParser();
				MALHandler handeler = new MALHandler();
				parser.parse(in, handeler);
				
				return handeler.getList();
			} catch (Exception e) {
				return null;
			}

		}

		protected void onPostExecute(LinkedList<AnimeRecord> result) {
				list.clear();
				list.addAll(result);
				adapter.notifyDataSetChanged();
		}

	}

}