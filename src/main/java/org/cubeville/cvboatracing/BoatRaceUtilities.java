package org.cubeville.cvboatracing;

import org.bukkit.Bukkit;
import org.cubeville.cvboatracing.models.Score;
import org.cubeville.cvboatracing.models.Track;

import java.util.ArrayList;
import java.util.List;

public class BoatRaceUtilities {
	public static String formatTimeString(long time) {
		return String.format("%d:%02d.%03d", (int) time / 60000, (int) (time / 1000) % 60, (int) time % 1000);
	}

	public static List<String> getLeaderboardLines(Track t, int startIndex, int endIndex) {
		List<String> result = new ArrayList<>();
		result.add("§e§lLeaderboard for §6§l" + t.getName());
		result.add("§f§l--------------------------------");
		if (ScoreManager.getTrackScores(t) == null) {
			result.add("§e§lThere are no scores on this track.");
		} else {
			List<Score> sortedTimes = new ArrayList<>(ScoreManager.getTrackScores(t));
			for (int i = startIndex; i <= endIndex; i++) {
				if (i >= sortedTimes.size()) {
					break;
				}
				Score s = sortedTimes.get(i);
				result.add("§e§l#" + (i + 1) + "§b " + s.getPlayerName() + " §f- " + formatTimeString(
					s.getFinalTime()));
			}
			if (result.size() == 2) {
				result.add("§7§oThere are no times for places " + (startIndex + 1) + " - " + (endIndex + 1)
					+ ".");
			}
		}
		result.add("§f§l--------------------------------");
		return result;
	}
}