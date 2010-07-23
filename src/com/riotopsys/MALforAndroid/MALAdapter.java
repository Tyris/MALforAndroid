package com.riotopsys.MALforAndroid;

import java.util.LinkedList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MALAdapter extends BaseAdapter {

	LinkedList<AnimeRecord> list;
	
	public MALAdapter(LinkedList<AnimeRecord> list) {
		this.list=list;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {		
		return list.get(position).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		StringBuffer sb = new StringBuffer();
		View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.mal_item, null);
        }
        AnimeRecord a = list.get(position);
        if (a != null) {
                TextView title = (TextView) v.findViewById(R.id.title);
                TextView score = (TextView) v.findViewById(R.id.score);
                TextView complete = (TextView) v.findViewById(R.id.complete);
                title.setText( a.title );
                
                sb.append( "Score: ").append(String.valueOf(a.score));                
                score.setText( sb.toString() );
                
                sb.delete(0, sb.length());
                sb.append(String.valueOf(a.watchedEpisodes)).append( "/ ").append(String.valueOf(a.episodes));                
                complete.setText( sb.toString() );
                
        }
        return v;
	}

	
	
	
}
