package com.riotopsys.MALforAndroid;

public class AnimeRecord {

	public int id;
	public String title;
	public String type;
	public String imageUrl;
	public int episodes;
	public String status;
	public int watchedEpisodes;
	public int score;
	public String watchedStatus;
	
	public String replaceStatement(){
		return "replace into `animeList` values ("+
			String.valueOf( id ) + ", "+
			addQuotes(title) + ", "+
			addQuotes(type) + ", "+
			addQuotes(imageUrl) + ", "+
			String.valueOf( episodes ) + ", "+
			addQuotes(status) + ", "+
			String.valueOf( watchedEpisodes ) + ", "+
			String.valueOf( score ) + ", "+
			addQuotes(watchedStatus) +	", 0);";
	}
	
	private String addQuotes( String s){
		return "\"" + s + "\""; 		
	}
	
}
