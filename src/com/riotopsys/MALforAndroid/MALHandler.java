package com.riotopsys.MALforAndroid;

import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;

public class MALHandler extends DefaultHandler {

	private StringBuffer sb;
	private LinkedList<AnimeRecord> list;
	private AnimeRecord currentAnime;

	MALHandler(  ) {
		
		sb = new StringBuffer();
		list = new LinkedList<AnimeRecord>();
		
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
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		super.endElement(uri, localName, name);
		String s = sb.toString().trim();		
		if (localName != null) {
	
			if (localName.equals("anime")) {
				list.add(currentAnime);
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
				currentAnime.watchedStatus= Html.fromHtml(s).toString();
			}
			sb.delete(0, sb.length());
		}
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		list.clear();		
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();		
	}

	public LinkedList<AnimeRecord> getList() {
		return list;
	}

}
