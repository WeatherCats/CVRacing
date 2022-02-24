package org.cubeville.cvracing.models;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.TripwireHook;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.PressureSensor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.cubeville.cvracing.*;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Race {
	private JavaPlugin plugin;
	private Track track;
	private Player player;
	private Score comparingTime;
	private Score personalBest;
	private HashMap<Integer, Long> splits = new HashMap<>();
	private int checkpointIndex;
	private int countdownTimer;
	private int stopwatch;
	private int minuteCap;
	private long elapsed;
	private ArmorStand armorStand;

	public Race(Track track, Player player, JavaPlugin plugin) {
		this.track = track;
		this.player = player;
 		this.plugin = plugin;
		this.checkpointIndex = 0;
		this.countdownTimer = 0;
		this.minuteCap = 10; // The player can go for x minutes before they are kicked out of the game
		this.personalBest = ScoreManager.getScore(player.getUniqueId(), track);
		this.comparingTime = determineComparingTime();

		this.startRace();
	}

	private Score determineComparingTime() {
		if (SelectedSplits.isUsingWR(player.getUniqueId())) {
			return ScoreManager.getWRScore(track);
		}

		UUID selectedSplitPlayer = SelectedSplits.getSelectedSplitPlayer(player.getUniqueId());
		if (selectedSplitPlayer != null) {
			return ScoreManager.getScore(selectedSplitPlayer, track);
		}
		return this.personalBest;
	}

	public void startRace() {
		this.track.setStatus(TrackStatus.IN_USE);
		TrackManager.clearPlayerFromQueues(player);
		if (!this.track.getSpawn().getChunk().isLoaded()) {
			this.track.getSpawn().getChunk().load();
		}
		player.teleport(this.track.getSpawn());
		Vehicle v = null;
		switch (this.track.getType()) {
			case BOAT:
				v = (Vehicle) this.player.getWorld().spawnEntity(this.track.getSpawn(), EntityType.BOAT);
				break;
			case PIG:
				Pig p = (Pig) this.player.getWorld().spawnEntity(this.track.getSpawn(), EntityType.PIG);
				p.setSaddle(true);
				this.player.getInventory().setItem(0, new ItemStack(Material.CARROT_ON_A_STICK));
				v = p;
				break;
			case HORSE:
				Horse h = (Horse) this.player.getWorld().spawnEntity(this.track.getSpawn(), EntityType.HORSE);
				h.getInventory().setSaddle(new ItemStack(Material.SADDLE, 1));
				h.setTamed(true);
				h.setOwner(this.player);
				h.setJumpStrength(.8);
				h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.5);
				v = h;
				break;
		}
		this.armorStand = (ArmorStand) Objects.requireNonNull(this.track.getSpawn().getWorld()).spawnEntity(this.track.getSpawn(), EntityType.ARMOR_STAND);
		this.armorStand.setVisible(false);
		this.armorStand.setGravity(false);
		this.armorStand.setCanPickupItems(false);
		this.armorStand.setMarker(true);
		this.armorStand.addScoreboardTag("CVBoatRace-LeaderboardArmorStand");
		if (v != null) {
			v.addPassenger(this.player);
			this.armorStand.addPassenger(v);
		} else {
			this.armorStand.addPassenger(this.player);
		}
		runCountdown(3);
	}

	public Location getCurrentCheckpoint() {
		return this.track.getCheckpoints().get(this.checkpointIndex);
	}

	public void advanceCheckpointIfShould() {
		Block b = this.getCurrentCheckpoint().getBlock();
		if (b.getBlockData() instanceof TripwireHook) {
			if (((Powerable) b.getBlockData()).isPowered()) {
				advanceCheckpoint();
			}
		} else {
			if (player.getLocation().distance(b.getLocation()) < 1.5) {
				advanceCheckpoint();
			}
		}
	}

	private void advanceCheckpoint() {
		if (checkpointIndex == this.track.getCheckpoints().size() - 1) {
			stopStopwatch();
			this.completeRace();
		} else if (checkpointIndex != 0) {
			splits.put(checkpointIndex, elapsed);
			player.sendMessage("§6CP" + checkpointIndex + ": " + RaceUtilities.formatTimeString(elapsed) + getSplitString(elapsed));
		}
		checkpointIndex++;
	}

	private String getSplitString(long currentTime) {
		if (comparingTime != null) {
			long comparingSplit = comparingTime.getSplit(checkpointIndex);
			String comparingName = "";
			if (!comparingTime.getPlayerUUID().equals(player.getUniqueId())) {
				comparingName = " -- " + comparingTime.getPlayerName();
			}
			if (comparingSplit > currentTime) {
				return " §6(§a-" + RaceUtilities.formatTimeString(comparingSplit - currentTime) + "§6)" + comparingName;
			} else if (comparingSplit < currentTime) {
				return " §6(§c+" + RaceUtilities.formatTimeString(currentTime - comparingSplit) + "§6)" + comparingName;
			} else {
				return " §6(§e00:00.00§6)" + comparingName;
			}
		}
		return "";
	}

	public void cancelRace(String subtitle) {
		player.sendTitle( ChatColor.RED + "Race ended", ChatColor.DARK_RED + subtitle, 5, 90, 5);
		if (this.countdownTimer != 0) { endCountdown(); }
		if (this.stopwatch != 0) { stopStopwatch(); }
		this.finishRace();
	}

	public void completeRace() {
		String pbString = " ";
		if (personalBest == null || personalBest.getFinalTime() > elapsed) {
			String pbBy = "";
			if (personalBest != null) {
				pbBy = ", which was your personal best by " + RaceUtilities.formatTimeString(personalBest.getFinalTime() - elapsed);
			}
			player.sendMessage("§bYou achieved a time of " + RaceUtilities.formatTimeString(elapsed) + " on " + track.getName() + pbBy + "!");
			pbString = "§a§lNew Personal Best!";
			Score wr = ScoreManager.getWRScore(track);
			ScoreManager.setNewPB(player.getUniqueId(), track, elapsed, splits);
			if (wr == null || elapsed < wr.getFinalTime()) {
				String broadcastString = "&b&l" + player.getName() + "&3 just got a new world record time of &b&l" + RaceUtilities.formatTimeString(elapsed) + "&3 on &b&l" + track.getName() + "&3!";
				//Bukkit.getServer().broadcastMessage("§b§l" + player.getName() + "§3 just got a new world record time of §b§l" + BoatRaceUtilities.formatTimeString(finalTime) + "§3 on §b§l" + track.getName() + "!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "runalias /announceboatswr " + broadcastString);
			} else {
				String broadcastString = "&d&l" + player.getName() + "&5 just got a new personal best time of &d&l" + RaceUtilities.formatTimeString(elapsed) + "&5, which put them at rank &d&l#" + ScoreManager.getScorePlacement(track, player.getUniqueId()) + "&5 on &d&l" + track.getName() + "&5!";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "runalias /announceboatspb " + broadcastString);
			}
			if (ScoreManager.shouldRefreshLeaderboard(elapsed, track)) {
				track.loadLeaderboards();
			}
		} else {
			player.sendMessage("§bYou achieved a time of " + RaceUtilities.formatTimeString(elapsed) + " on " + track.getName() + ", which was " + RaceUtilities.formatTimeString(elapsed - personalBest.getFinalTime()) + " behind your personal best!");
		}
		player.sendTitle("§d§l" + RaceUtilities.formatTimeString(this.elapsed), pbString, 5, 90, 5);
		this.finishRace();
	}

	public void finishRace() {
		if (track.getQueue().size() == 0) {
			track.setStatus(TrackStatus.OPEN);
		}
		RaceManager.removeRace(player, track);
		Entity v = player.getVehicle();
		player.getInventory().clear();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!this.track.getExit().getChunk().isLoaded()) {
				this.track.getExit().getChunk().load();
			}
			player.teleport(track.getExit());
			if (v != null) { v.remove(); }
		}, 1L);

	}

	private void runCountdown(int startCount) {
		this.countdownTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
			int counter = startCount;
			@Override
			public void run() {
				if (counter > 0) {
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 3.0F, 0.7F);
					player.sendTitle(ChatColor.RED + String.valueOf(counter), null, 1, 18, 1);
					counter--;
				} else {
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 3.0F, 1.4F);
					player.sendTitle(ChatColor.GREEN + "Go!", null, 1, 38, 1);
					endCountdown();
				}
			}
		}, 0L, 20L);
	}

	private void endCountdown() {
		Bukkit.getScheduler().cancelTask(this.countdownTimer);
		countdownTimer = 0;
		this.armorStand.eject();
		this.armorStand.remove();
		this.armorStand = null;
		startStopwatch();
	}

	public boolean isCountingDown() {
		return countdownTimer != 0;
	}

	private void startStopwatch() {
		this.elapsed = 0;
		this.stopwatch = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
			this.elapsed = this.elapsed + 50;
			if ((int) elapsed / 60000 >= minuteCap) { cancelRace("You took too long to finish.");}
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§a§l" + RaceUtilities.formatTimeString(elapsed)));
		}, 0L, 1L);
	}

	private void stopStopwatch() {
		Bukkit.getScheduler().cancelTask(this.stopwatch);
		stopwatch = 0;
	}

	public Track getTrack() {
		return track;
	}
}