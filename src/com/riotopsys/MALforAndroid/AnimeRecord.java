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

	public String memberScore;
	public String rank;
	public String synopsis;

	public void animeRecord() {
		memberScore = null;
		rank = null;
		synopsis = null;
	}

	public String insertStatement() {
		return "insert into `animeList` values (" + String.valueOf(id) + ", " + addQuotes(title) + ", " + addQuotes(type) + ", " + addQuotes(imageUrl) + ", "
				+ String.valueOf(episodes) + ", " + addQuotes(status) + ", " + String.valueOf(watchedEpisodes) + ", " + String.valueOf(score) + ", "
				+ addQuotes(watchedStatus) + ", 0,NULL,NULL,NULL)";
	}

	public String updateStatement() {
		StringBuffer sb = new StringBuffer();
		sb.append("update animeList set ");
		sb.append("title = ").append(addQuotes(title)).append(", ");
		sb.append("type = ").append(addQuotes(type)).append(", ");
		sb.append("imageUrl = ").append(addQuotes(imageUrl)).append(", ");
		sb.append("episodes = ").append(String.valueOf(episodes)).append(", ");
		sb.append("status = ").append(addQuotes(status)).append(", ");
		sb.append("watchedEpisodes = ").append(String.valueOf(watchedEpisodes)).append(", ");
		sb.append("score = ").append(String.valueOf(score)).append(", ");
		sb.append("watchedStatus = ").append(addQuotes(watchedStatus)).append(", ");
		sb.append("dirty = 0 ");

		sb.append("where id = ").append(String.valueOf(id));

		return sb.toString();
	}

	public String updateExtras() {
		String result = null;
		if (memberScore != null) {
			StringBuffer sb = new StringBuffer();
			sb.append("update animeList set ");
			sb.append("memberScore = ").append(addQuotes(memberScore)).append(", ");
			sb.append("rank = ").append(addQuotes(rank)).append(", ");
			sb.append("synopsis = ").append(addQuotes(synopsis));// .append(", ");

			sb.append(" where id = ").append(String.valueOf(id));
			result = sb.toString();
		}
		return result;
	}

	private String addQuotes(String s) {
		return "'" + s.replace("'", "''") + "'";
	}

}
