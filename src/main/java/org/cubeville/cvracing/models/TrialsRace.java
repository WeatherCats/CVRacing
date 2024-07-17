package org.cubeville.cvracing.models;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvracing.*;

import java.util.Map;
import java.util.UUID;

public class TrialsRace extends Race {

    private Player player;
    private Score comparingTime;
    private Score personalBest;

    public TrialsRace(Track track, JavaPlugin plugin, Player player) {
        super(track, plugin, 1);
        this.player = player;
        this.personalBest = ScoreManager.getScore(player.getUniqueId(), track);
        this.comparingTime = determineComparingTime();
    }

    public void start() {
        this.startRace();
    }

    private Score determineComparingTime() {
        if (SelectedSplits.isUsingWR(player.getUniqueId())) {
            return ScoreManager.getWRScore(this.getTrack());
        }

        UUID selectedSplitPlayer = SelectedSplits.getSelectedSplitPlayer(player.getUniqueId());
        if (selectedSplitPlayer != null) {
            return ScoreManager.getScore(selectedSplitPlayer, this.getTrack());
        }
        return this.personalBest;
    }

    protected void startRace() {
        raceStates.put(player, new RaceState(player));
        this.getTrack().setStatus(TrackStatus.IN_USE);
        this.setupPlayerOnTrack(player, this.getTrack().getTrialsSpawn());
        hasStarted = true;
        runCountdown(player, 3);
        RaceUtilities.sendMetricToCVStats("race_start", Map.of(
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "trials"
        ));
        RaceUtilities.sendMetricToCVStats("player_race_start", Map.of(
            "player", player.getUniqueId().toString(),
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "trials"
        ));
    }

    protected String getSplitString(Player p, long currentTime) {
        if (comparingTime != null) {
            long comparingSplit = comparingTime.getSplit(this.raceStates.get(p).getCheckpointIndex() - 1);
            String comparingName = "";
            if (!comparingTime.getPlayerUUID().equals(p.getUniqueId())) {
                comparingName = " -- " + comparingTime.getPlayerName();
            }
            if (comparingSplit > currentTime) {
                return " §6(§a-" + RaceUtilities.formatTimeString(comparingSplit - currentTime) + "§6)" + comparingName;
            } else if (comparingSplit < currentTime) {
                return " §6(§c+" + RaceUtilities.formatTimeString(currentTime - comparingSplit) + "§6)" + comparingName;
            } else {
                return " §6(§e00:00.000§6)" + comparingName;
            }
        }
        return "";
    }

    public void completeRace(Player p) {
        String pbString = " ";
        long finalTime = raceStates.get(p).getFinishTime();
        if (personalBest == null || personalBest.getFinalTime() > finalTime) {
            String pbBy = "";
            if (personalBest != null) {
                pbBy = ", which was your personal best by " + RaceUtilities.formatTimeString(personalBest.getFinalTime() - finalTime);
            }
            p.sendMessage("§bYou achieved a time of " + RaceUtilities.formatTimeString(finalTime) + " on " + track.getName() + pbBy + "!");
            pbString = "§a§lNew Personal Best!";
            Score wr = ScoreManager.getWRScore(track);
            ScoreManager.setNewPB(p.getUniqueId(), track, finalTime, raceStates.get(p).getSplits());
            if (wr == null || finalTime < wr.getFinalTime()) {
                String broadcastString = "§b§l" + p.getName() + "§3 just got a new world record time of §b§l" + RaceUtilities.formatTimeString(finalTime) + "§3 on §b§l" + track.getName() + "§3!";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "runalias /announceracewr " + broadcastString);
            } else {
                String broadcastString = "§d§l" + p.getName() + "§5 just got a new personal best time of §d§l" + RaceUtilities.formatTimeString(finalTime) + "§5, which put them at rank §d§l#" + ScoreManager.getScorePlacement(track, p.getUniqueId()) + "§5 on §d§l" + track.getName() + "§5!";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "runalias /announceracepb " + broadcastString);
            }
            if (ScoreManager.shouldRefreshLeaderboard(finalTime, track)) {
                track.loadLeaderboards();
            }
        } else {
            p.sendMessage("§bYou achieved a time of " + RaceUtilities.formatTimeString(finalTime) + " on " + track.getName() + ", which was " + RaceUtilities.formatTimeString(finalTime - personalBest.getFinalTime()) + " behind your personal best!");
        }
        p.sendTitle("§d§l" + RaceUtilities.formatTimeString(finalTime), pbString, 5, 90, 5);
        RaceUtilities.sendMetricToCVStats("race_finish", Map.of(
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "trials"
        ));
        RaceUtilities.sendMetricToCVStats("player_race_finish", Map.of(
            "player", player.getUniqueId().toString(),
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "trials"
        ));
        this.endPlayerRace(p, true);
    }

    protected void endPlayerRace(Player p, boolean finished) {
        RaceManager.removeRace(p);
        p.getActivePotionEffects().forEach(potionEffect -> p.removePotionEffect(potionEffect.getType()));
        hasStarted = false;
        Location loc = track.getExit();
        if (finished) {
            if (track.getFinishExit() != null) {
                loc = track.getFinishExit();
            }
        }
        this.removePlayerFromRaceAndSendToLoc(p, loc);
        RaceManager.finishRace(track);
    }
}
