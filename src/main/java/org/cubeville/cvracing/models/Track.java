package org.cubeville.cvracing.models;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.checkerframework.checker.units.qual.A;
import org.cubeville.cvracing.*;

import java.util.*;

public class Track implements Listener {
	private String name;
	private String displayName;
	private TrackStatus status;
	private TrackType type = TrackType.BOAT;
	private VersusRace versusRace;
	private HostedRace hostedRace;
	private Location trialsSpawn;
	private List<Location> versusSpawns = new ArrayList<>();
	private Location exit;
	private Location finishExit;
	private Location spectate;
	private List<RaceSign> signs = new ArrayList<>();
	private List<Checkpoint> checkpoints = new ArrayList<>();
	private List<Leaderboard> leaderboards = new ArrayList<>();
	private Queue<Player> queue = new LinkedList<>();
	private boolean includeReset = false;
	private boolean isClosed = false;
	private boolean isSurvival = false;
	private boolean groundFails = false;

	public Track(String name) {
		this.name = name;
		this.status = TrackStatus.OPEN;
	}

	public Location getExit() {
		return this.exit;
	}

	public void setExit(Location exit) {
		this.exit = exit;
	}
	
	public Location getFinishExit() {
		return this.finishExit;
	}
	
	public void setFinishExit(Location finishExit) {
		this.finishExit = finishExit;
	}

	public Location getTrialsSpawn() {
		return this.trialsSpawn;
	}

	public void setTrialsSpawn(Location trialsSpawn) {
		this.trialsSpawn = trialsSpawn;
	}

	public String getName() {
		return name;
	}

	public TrackStatus getStatus() {
		return status;
	}

	public void setStatus(TrackStatus status) {
		this.status = status;
		for (RaceSign sign : this.signs) {
			sign.displayStatus(status);
		}
	}

	public void addSign(RaceSign raceSign) {
		this.signs.add(raceSign);
	}
	public void addCheckpoint(Checkpoint checkpoint) { this.checkpoints.add(checkpoint); }
	public void removeCheckpoint(int index) { this.checkpoints.remove(index); }
	public void removeSign(Location loc) {
		List<RaceSign> newSigns = new ArrayList<>();
		for (RaceSign sign: signs) {
			if (!loc.equals(sign.getSign().getLocation())) {
				newSigns.add(sign);
			}
		}
		this.signs = newSigns;
	}

	public void addLeaderboard(Location lbLoc) {
		leaderboards.add(new Leaderboard(lbLoc));
	}
	public void removeLeaderboard(int index) {
		this.leaderboards.get(index).clear();
		this.leaderboards.remove(index);
	}

	public void removeVersusSpawn(int index) {
		this.versusSpawns.remove(index);
	}

	public void loadLeaderboards() {
		leaderboards.forEach(leaderboard -> leaderboard.setDisplayText(RaceUtilities.getLeaderboardLines(this, 0, 9)));
	}

	public List<Leaderboard> getLeaderboards() {
		return leaderboards;
	}

	public void clearLeaderboards() {
		leaderboards.forEach(Leaderboard::clear);
	}

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	public void onRightClick(Player p, RaceSignType type, int lapNumber) {
		if (type != RaceSignType.EXIT) {
			if (hostedRace != null) {
				RaceManager.addPlayerToHostedLobby(this, p);
				return;
			}
			if (RaceManager.getRace(p) != null) {
				p.sendMessage(ChatColor.RED + "You are already in a race!");
				return;
			}
		}

		switch (type) {
			case TRIALS:
				switch (status) {
					case OPEN:
						RaceManager.addTrialsRace(this, p);
						break;
					case IN_USE:
					case IN_LOBBY:
						this.addToQueue(p);
						break;
					case CLOSED:
						p.sendMessage(ChatColor.RED + "This track is currently closed. Please try again later.");
						break;
				}
				break;
			case VERSUS:
				switch (status) {
					case OPEN:
						RaceManager.addVersusRace(this, p, lapNumber);
						break;
					case IN_USE:
						p.sendMessage(ChatColor.RED + "Please wait until the track is open to start a multiplayer game");
						break;
					case IN_LOBBY:
						RaceManager.addPlayerToVersus(this, p);
						break;
					case CLOSED:
						p.sendMessage(ChatColor.RED + "This track is currently closed. Please try again later.");
						break;
				}
				break;
			case ERROR:
				p.sendMessage(ChatColor.RED + "There was an error setting up this track sign, please contact a server administrator.");
				break;
			case SPECTATE:
				p.teleport(spectate);
				break;
			case EXIT:
				p.teleport(exit);
				RaceManager.removePlayerFromHostedLobby(this, p);
				break;
		}
	}

	private void addToQueue(Player p) {
		if (queue.contains(p)) {
			p.sendMessage(ChatColor.RED + "You are already in the queue!");
			return;
		}
		queue.add(p);
		p.sendMessage(ChatColor.YELLOW + "You have been been added to the queue!");
		for (RaceSign sign : this.signs) {
			sign.displayQueue();
		}
		TrackManager.getTracks().forEach(track -> RaceManager.removePlayerFromVersus(track, p));
	}

	public Queue<Player> getQueue() {
		return queue;
	}

	public void clearQueue() {
		queue.clear();
	}

	public Player getPlayerFromQueue() {
		return queue.poll();
	}

	public List<RaceSign> getSigns() {
		return signs;
	}

	public void removePlayerFromQueue(Player p) {
		queue.remove(p);
		if (versusRace != null) { versusRace.removePlayerFromState(p); }
	}

	public TrackType getType() {
		return type;
	}

	public void setType(TrackType type) {
		this.type = type;
	}

	public void setSpectate(Location spectate) {
		this.spectate = spectate;
	}

	public VersusRace getVersusRace() {
		return versusRace;
	}

	public void setVersusRace(VersusRace versusRace) {
		this.versusRace = versusRace;
	}

	public List<Location> getVersusSpawns() {
		return versusSpawns;
	}

	public void addVersusSpawn(Location versusSpawn) {
		this.versusSpawns.add(versusSpawn);
	}

	public boolean isIncludeReset() {
		return includeReset;
	}

	public void setIncludeReset(boolean includeReset) {
		this.includeReset = includeReset;
	}
	
	public boolean isSurvival() {
		return isSurvival;
	}
	
	public void setSurvival(boolean isSurvival) {
		this.isSurvival = isSurvival;
	}
	
	public boolean isGroundFails() {
		return groundFails;
	}
	
	public void setGroundFails(boolean groundFails) {
		this.groundFails = groundFails;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean closed) {
		isClosed = closed;
	}

	public void setHostedRace(HostedRace hostedRace) {
		this.hostedRace = hostedRace;
	}

	public HostedRace getHostedRace() {
		return hostedRace;
	}

	public Location getSpectate() {
		return spectate;
	}
}
