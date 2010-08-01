package com.riotopsys.MALforAndroid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.util.Log;

public class MALHandler extends DefaultHandler {

	private StringBuffer sb;
	private AnimeRecord currentAnime;
	private SQLiteDatabase db;
	private boolean extras;

	MALHandler(SQLiteDatabase db) {
		sb = new StringBuffer();
		this.db = db;
		extras = true;

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		sb.append(new String(ch, start, length));
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		if (localName.equals("anime")) {
			currentAnime = new AnimeRecord();
		} else if (localName.equals("animelist")) {
			extras = false;
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		super.endElement(uri, localName, name);
		String s = sb.toString().trim();
		if (localName != null) {
			try {
				if (localName.equals("anime")) {

					if (!extras) {
						try {
							// db.insertOrThrow("animeList", null, values);
							db.execSQL(currentAnime.insertStatement());
						} catch (Exception e) {
							// Log.i("MALHandler", "insert", e);
							try {
								db.execSQL(currentAnime.updateStatement());
							} catch (Exception e2) {
								Log.i("MALHandler", "update all", e2);
							}
						}
					} else {
						try {
							db.execSQL(currentAnime.updateExtras());
						} catch (Exception e2) {
							Log.i("MALHandler", "update extras", e2);
						}
					}

					// Log.e("MALHandler:endElement", "Added: "+
					// currentAnime.title);
					currentAnime = null;

				} else if (localName.equals("id")) {
					currentAnime.id = Integer.parseInt(s);
				} else if (localName.equals("title")) {
					currentAnime.title = Html.fromHtml(s).toString();
				} else if (localName.equals("type")) {
					currentAnime.type = Html.fromHtml(s).toString();
				} else if (localName.equals("image_url")) {
					currentAnime.imageUrl = s;
				} else if (localName.equals("episodes")) {
					currentAnime.episodes = Integer.parseInt(s);
				} else if (localName.equals("status")) {
					currentAnime.status = Html.fromHtml(s).toString();
				} else if (localName.equals("watched_episodes")) {
					currentAnime.watchedEpisodes = Integer.parseInt(s);
				} else if (localName.equals("score")) {
					currentAnime.score = Integer.parseInt(s);
				} else if (localName.equals("watched_status")) {
					currentAnime.watchedStatus = Html.fromHtml(s).toString();
				} else if (localName.equals("members_score")) {
					currentAnime.memberScore = Html.fromHtml(s).toString();
				} else if (localName.equals("rank")) {
					currentAnime.rank = Html.fromHtml(s).toString();
				} else if (localName.equals("synopsis")) {
					currentAnime.synopsis = Html.fromHtml(s).toString();
				}
			} catch (Exception e) {
				Log.e("MALHandler", "endTag", e);
			}
			sb.delete(0, sb.length());
		}

	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

}
