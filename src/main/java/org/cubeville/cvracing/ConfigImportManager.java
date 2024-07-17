package org.cubeville.cvracing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvracing.models.CPRegion;
import org.cubeville.cvracing.models.Checkpoint;
import org.cubeville.cvracing.models.Track;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ConfigImportManager {

	public static void importConfiguration(JavaPlugin plugin) {
		FileConfiguration config = plugin.getConfig();
		if (config.getString("timing") != null) {
			RaceManager.setTiming(config.getString("timing"));
		}
		if (config.getConfigurationSection("tracks") == null) {
			return;
		}

		for (String trackName : Objects.requireNonNull(config.getConfigurationSection("tracks")).getKeys(false)) {
			Track track = TrackManager.addTrack(trackName);
			ConfigurationSection trackConfig = config.getConfigurationSection("tracks." + trackName);
			assert trackConfig != null;
			String spawn = trackConfig.getString("trialsSpawn");
			if (spawn != null) { track.setTrialsSpawn( parseTeleportLocation(spawn) ); }
			String exit = trackConfig.getString("exit");
			if (exit != null) { track.setExit( parseTeleportLocation(exit) ); }
			String finishExit = trackConfig.getString("finishexit");
			if (finishExit != null) { track.setFinishExit( parseTeleportLocation(finishExit) ); }
			String spectate = trackConfig.getString("spectate");
			if (spectate != null) { track.setSpectate( parseTeleportLocation(spectate) ); }
			boolean canResetToCp = trackConfig.getBoolean("tptocp");
			track.setIncludeReset(canResetToCp);
			boolean isSurvivalMode = trackConfig.getBoolean("survival");
			track.setSurvival(isSurvivalMode);
			boolean isGroundFails = trackConfig.getBoolean("groundfails");
			track.setGroundFails(isGroundFails);
			String type = trackConfig.getString("type");
			if (type != null) {
				try { TrackType tt = TrackType.valueOf(type.toUpperCase()); track.setType(tt); }
				// don't worry if the type doesn't match, just use the default race type (boats)
				catch (IllegalArgumentException|NullPointerException ignored) {}
			}
			boolean isClosed = trackConfig.getBoolean("isClosed");
			if (isClosed) {
				track.setClosed(true);
				track.setStatus(TrackStatus.CLOSED);
			}

			ConfigurationSection checkpoints = trackConfig.getConfigurationSection("checkpoints");
			if (checkpoints != null) {
				int i = 0;
				while (checkpoints.contains(Integer.toString(i))) {
					Checkpoint cp = new Checkpoint();
					for (String cpRegion : Objects.requireNonNull(checkpoints.getConfigurationSection(String.valueOf(i))).getKeys(false)) {
						if (cpRegion.equals("variables")) {
							if (checkpoints.contains(i + ".variables.commands")) {
								cp.setCommands(checkpoints.getStringList(i + ".variables.commands"));
							}
							continue;
						}
						String[] minMax = cpRegion.split("~");
						CPRegion cpr = cp.addRegion(parseBlockLocation(minMax[0]), parseBlockLocation(minMax[1]));
						if (checkpoints.contains(i + "." + cpRegion + ".reset")) {
							cpr.setReset(parseTeleportLocation(Objects.requireNonNull(checkpoints.getString(i + "." + cpRegion + ".reset"))));
						}
					}
					track.addCheckpoint(cp);
					i++;
				}
			}

			List<String> leaderboardLocStrings = trackConfig.getStringList("leaderboards");
			for (String lbLocString : leaderboardLocStrings) {
				Location lbLoc = parseTeleportLocation(lbLocString);

				List<Entity> nearbyEntities = (List<Entity>) Objects.requireNonNull(lbLoc.getWorld())
					.getNearbyEntities(lbLoc, 2, 5, 2);

				// fail safe if any of the armor stands make it to enabling of the plugin
				for (Entity ent : nearbyEntities) {
					if (ent.getScoreboardTags().contains("CVBoatRace-LeaderboardArmorStand")) {
						ent.remove();
					}
				}
				track.addLeaderboard(lbLoc);
			}

			List<String> trialsSpawnLocStrings = trackConfig.getStringList("versusSpawns");
			for (String trialsSpawnLocString : trialsSpawnLocStrings) {
				Location tsLoc = parseTeleportLocation(trialsSpawnLocString);
				track.addVersusSpawn(tsLoc);
			}

			try {
				Set<String> signLocStrings = Objects.requireNonNull(trackConfig.getConfigurationSection("signs")).getKeys(false);
				for (String signLocString : signLocStrings) {
					Location signLoc = parseBlockLocation(signLocString);
					if (SignManager.signMaterials.contains(signLoc.getBlock().getType())) {
						Sign sign = (Sign) signLoc.getBlock().getState();
						try {
							RaceSignType rt = RaceSignType.valueOf(Objects.requireNonNull(trackConfig.getString("signs." + signLocString + ".type")).toUpperCase());
							SignManager.addSign(sign, track, rt);
						} catch (IllegalArgumentException | NullPointerException e) {
							SignManager.addSign(sign, track, RaceSignType.ERROR);
						}
						final int lapCount = trackConfig.getInt("signs." + signLocString + ".laps");
						if (lapCount > 1) {
							SignManager.getSign(signLoc).setLaps(lapCount);
						}
					}
				}
			} catch (IllegalArgumentException|NullPointerException ignored) {}
		}
	}

	private static Location parseBlockLocation(String s) {
		List<String> params = Arrays.asList(s.split(","));
		return new Location(
			Bukkit.getWorld(params.get(0)), // world
			Integer.parseInt(params.get(1)), // x
			Integer.parseInt(params.get(2)), // y
			Integer.parseInt(params.get(3)) // z
		);
	}

	private static Location parseTeleportLocation(String s) {
		List<String> params = Arrays.asList(s.split(","));
		return new Location(
			Bukkit.getWorld(params.get(0)), // world
			Float.parseFloat(params.get(1)), // x
			Float.parseFloat(params.get(2)), // y
			Float.parseFloat(params.get(3)), // z
			Float.parseFloat(params.get(4)), // pitch
			Float.parseFloat(params.get(5)) // yaw
		);
	}
}
