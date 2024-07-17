package org.cubeville.cvracing.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.cubeville.cvracing.RaceManager;
import org.cubeville.cvracing.RaceUtilities;
import org.cubeville.cvracing.TrackStatus;

import java.util.*;
import java.util.stream.Collectors;

public class VersusRace extends Race {
    private int lobbyTimeout;
    private int lobbyCountdown = 0;
    private int countdownValue;
    public int maxPlayers;
    // I'm stopping at 32nd, I do not like places lol
    private final String[] placeStrings = {
            "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
            "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th",
            "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st", "32nd"
    };

    private List<RaceState> sortedRaceStates = new ArrayList<>();
    final int LOBBY_TIMEOUT_MINUTES = 1;

    public VersusRace(Track track, JavaPlugin plugin, int laps) {
        super(track, plugin, laps);
        this.maxPlayers = track.getVersusSpawns().size();
        startLobbyTimeout();
    }

    public void addPlayer(Player p) {
        raceStates.put(p, new RaceState(p));
        raceStates.keySet().forEach(player -> player.sendMessage("§e" + p.getDisplayName() + "§6 has joined the race lobby"));

        if (playerSize() > 1 && lobbyCountdown == 0) {
            setLobbyCountdown(15);
        }

        if (playerSize() >= maxPlayers) {
            raceStates.keySet().forEach(player -> player.sendMessage("§b§lThis lobby is now full! Starting countdown."));
            if (countdownValue > 5) {
                setLobbyCountdown(5);
            }
        }
    }

    private void cancelLobbyTimeout() {
        if (lobbyTimeout != 0) {
            Bukkit.getScheduler().cancelTask(lobbyTimeout);
            lobbyTimeout = 0;
        }
    }

    protected void startLobbyTimeout() {
        lobbyTimeout = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            for (Player p : raceStates.keySet()) {
                p.sendMessage("§cThe queue for this race has timed out.");
            }
            this.endRace();
            cancelLobbyTimeout();
        }, LOBBY_TIMEOUT_MINUTES * 60000);
    }

    protected void setLobbyCountdown(int time) {
        cancelLobbyCountdown();
        countdownValue = time;
        lobbyCountdown =  Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            if (countdownValue == 0) {
                cancelLobbyCountdown();
                startRace();
            } else if (countdownValue <= 5) {
                raceStates.keySet().forEach(player -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2F, 1F);
                    player.sendMessage("§bRace starting in " + countdownValue + "...");
                });
            } else if (countdownValue % 15 == 0) {
                raceStates.keySet().forEach(player -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2F, 1F);
                    player.sendMessage("§bRace starting in " + countdownValue + " seconds");
                });
            }
            countdownValue--;
        }, 0L, 20L);
    }

    private void cancelLobbyCountdown() {
        if (lobbyCountdown != 0) {
            Bukkit.getScheduler().cancelTask(lobbyCountdown);
            lobbyCountdown = 0;
        }
    }

    public void removePlayer(Player p) {
        if (!hasStarted) {
            raceStates.keySet().forEach(player -> player.sendMessage("§e" + p.getDisplayName() + "§6 has left the race lobby"));
            raceStates.remove(p);
            switch (playerSize()) {
                case 1:
                    startLobbyTimeout();
                    cancelLobbyCountdown();
                    raceStates.keySet().forEach(player -> player.sendMessage("§cThere are not enough players to start this race!"));
                    break;
                case 0:
                    cancelLobbyTimeout();
                    this.endRace();
                    break;
            }
            track.getSigns().forEach(RaceSign::displayQueue);
        }
        RaceManager.removeRace(p);
    }

    public void removePlayerFromState(Player p) {
        raceStates.remove(p);
        p.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard());
    }

    protected void startRace() {
        this.getTrack().setStatus(TrackStatus.IN_USE);
        int i = 0;
        updateSortedRaceStates();
        Scoreboard scoreboard = getRaceScoreboard();
        hasStarted = true;
        for (Player p : raceStates.keySet()) {
            this.setupPlayerOnTrack(p, track.getVersusSpawns().get(i));
            runCountdown(p, 3);
            p.setScoreboard(scoreboard);
            RaceUtilities.sendMetricToCVStats("player_race_start", Map.of(
                "player", p.getUniqueId().toString(),
                "track", this.getTrack().getName(),
                "type", this.getTrack().getType().name().toLowerCase(),
                "gamemode", "versus"
            ));
            i++;
        }
        RaceUtilities.sendMetricToCVStats("race_start", Map.of(
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "versus"
        ));
    }

    protected Scoreboard getRaceScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        String objName = "race-" + track.getName();
        objName = objName.substring(0, Math.min(objName.length(), 16));
        Objective raceObjective = scoreboard.registerNewObjective(objName, "dummy", "§b§lRace on §e§l" + track.getName());
        raceObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < sortedRaceStates.size(); i++) {
            RaceState currentRaceState = sortedRaceStates.get(i);
            if (currentRaceState.isCanceled()) {
                raceObjective.getScore("§cDNF§b: §e" + currentRaceState.getPlayer().getDisplayName()).setScore(sortedRaceStates.size() - i);
                continue;
            }
            int cpIndex = currentRaceState.getCheckpointIndex();

            String behindSplit = getBehindSplit(currentRaceState, cpIndex, i);
            String CPString = currentRaceState.getFinishTime() == 0 ? " §f[Lap " + (currentRaceState.getLapIndex() + 1) + " CP " + cpIndex + "]" : " §a§l[Finished]";
            String entry = getColorByIndex(i) + placeStrings[i] + "§f: §e"
                + sortedRaceStates.get(i).getPlayer().getDisplayName()
                + behindSplit + CPString;

            raceObjective.getScore(entry).setScore(sortedRaceStates.size() - i);
        }
        return scoreboard;
    }

    private String getBehindSplit(RaceState currentRaceState, int cpIndex, int i) {
        int splitIndex = (currentRaceState.getLapIndex() * track.getCheckpoints().size()) + cpIndex - 1;
        if (i == 0 || splitIndex == -1) { return ""; }
        long behindDiff;
        if (currentRaceState.getFinishTime() == 0) {
            long prevSplit = currentRaceState.getSplit(splitIndex);
            long otherPrevSplit = sortedRaceStates.get(i - 1).getSplit(splitIndex);
            if (prevSplit < otherPrevSplit) { return ""; }
            behindDiff = prevSplit - otherPrevSplit;
        } else {
            behindDiff = currentRaceState.getFinishTime() - sortedRaceStates.get(i - 1).getFinishTime();
        }
        return " §b§l+" + RaceUtilities.formatTimeString(behindDiff);
    }

    public boolean hasPlayer(Player p) {
        return raceStates.containsKey(p);
    }

    public int playerSize() {
        return raceStates.size();
    }

    @Override
    public void completeRace(Player player) {
        long elapsed = this.raceStates.get(player).getElapsed();
        updateSortedRaceStates();
        int placement = sortedRaceStates.indexOf(this.raceStates.get(player));
        String timeToAhead = "";
        if (placement != 0) {
            Player playerAhead = sortedRaceStates.get(placement - 1).getPlayer();
            timeToAhead = "§b which was "
                    + RaceUtilities.formatTimeString(elapsed - this.raceStates.get(playerAhead).getFinishTime())
                    + " behind " + playerAhead.getDisplayName();
        }
        player.sendMessage("§bYou completed the race in " + getColorByIndex(placement) + placeStrings[placement] + " place§b!");
        player.sendMessage("§bYou had a time of §n" + RaceUtilities.formatTimeString(elapsed) + timeToAhead);
        RaceUtilities.sendMetricToCVStats("player_race_finish", Map.of(
            "player", player.getUniqueId().toString(),
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "versus"
        ));
        this.endPlayerRace(player, true);
    }

    @Override
    protected String getSplitString(Player player, long elapsed) {
        updateSortedRaceStates();
        RaceState pState = this.raceStates.get(player);
        updateScoreboard();
        int placement = sortedRaceStates.indexOf(pState);

        String timeToAhead = "";
        if (placement != 0) {
            Player playerAhead = sortedRaceStates.get(placement - 1).getPlayer();
            int splitIndex = (pState.getLapIndex() * track.getCheckpoints().size()) + pState.getCheckpointIndex() - 1;
            timeToAhead = "§6 "
                    + RaceUtilities.formatTimeString(elapsed
                    - this.raceStates.get(playerAhead).getSplit(splitIndex)
                    ) + " behind " + playerAhead.getDisplayName();
        }
        return " -- " + getColorByIndex(placement) + placeStrings[placement] + timeToAhead;
    }

    protected void updateScoreboard() {
        Scoreboard scoreboard = getRaceScoreboard();
        this.raceStates.keySet().forEach(p -> p.setScoreboard(scoreboard));
    }

    protected void updateSortedRaceStates() {
        this.sortedRaceStates = this.raceStates.values().stream()
        .filter(v -> !v.isSpectator() || v.getFinishTime() != 0)
        .sorted(new RaceStateComparator(track.getCheckpoints().size()))
        .collect(Collectors.toList());
    }

    protected String getColorByIndex(int index) {
        switch (index) {
            case 0:
                return "§e§l";
            case 1:
                return "§7§l";
            case 2:
                return "§6§l";
            default:
                return "";
        }
    }

    protected List<String> finalResults() {
        List<String> finalResults = new ArrayList<>();
        finalResults.add("§b§lFinal results on §e§l" + track.getName());
        int i = 0;
        for (RaceState rs : sortedRaceStates) {
            if (rs.getFinishTime() != 0) {
                finalResults.add("§b" + getColorByIndex(i) + placeStrings[i]
                + "§b: §e" + rs.getPlayer().getDisplayName() + "§f -- §b" + RaceUtilities.formatTimeString(rs.getFinishTime()));
            } else if (rs.isCanceled()) {
                finalResults.add("§cDNF§b: §e" + rs.getPlayer().getDisplayName());
            } else {
                finalResults.add("§cSomething went wrong processing " + rs.getPlayer().getDisplayName());
            }
            i++;
        }
        return finalResults;
    }

    protected Location endLocation() {
        return track.getExit();
    }

    @Override
    protected void endPlayerRace(Player player, boolean finished) {
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        removePlayer(player);
        this.removePlayerFromRaceAndSendToLoc(player, endLocation());
        updateScoreboard();
        for (RaceState rs : raceStates.values()) {
            if (rs.getFinishTime() == 0 && !rs.isCanceled() && !rs.isSpectator()) {
                return;
            }
        }

        RaceUtilities.sendMetricToCVStats("race_finish", Map.of(
            "track", this.getTrack().getName(),
            "type", this.getTrack().getType().name().toLowerCase(),
            "gamemode", "versus"
        ));

        hasStarted = false;
        this.raceStates.keySet().forEach(p -> {
            finalResults().forEach(p::sendMessage);
            p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        });
        this.endRace();
    }

    protected void endRace() {
        this.raceStates.clear();
        RaceManager.finishRace(track);
    }
}
