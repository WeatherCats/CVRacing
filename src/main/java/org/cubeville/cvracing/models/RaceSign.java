package org.cubeville.cvracing.models;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.cubeville.cvracing.RaceSignType;
import org.cubeville.cvracing.TrackStatus;

public class RaceSign {

	private final Sign sign;
	private final Track track;
	private RaceSignType type;
	private int laps = 1;

	public RaceSign(Sign sign, Track track, RaceSignType type) {
		this.sign = sign;
		this.track = track;
		Sign editSign = (Sign) this.sign.getLocation().getBlock().getState();
		editSign.getSide(Side.FRONT).setLine(1, track.getName());
		editSign.update(true);
		this.setType(type);
		this.displayQueue();
		this.displayStatus(track.getStatus());
	}

	public void displayType() {
		Sign editSign = (Sign) this.sign.getLocation().getBlock().getState();
		if (track.getHostedRace() != null && type != RaceSignType.ERROR && type != RaceSignType.EXIT) {
			editSign.getSide(Side.FRONT).setLine(0, ChatColor.YELLOW + "" + ChatColor.BOLD + "[HOSTING]"); //§e§l
			editSign.update();
			return;
		}
		String typeText = "";
		switch (type) {
			case TRIALS:
				typeText += ChatColor.AQUA; //§b
				break;
			case VERSUS:
				typeText += ChatColor.LIGHT_PURPLE; //§d
				break;
			case SPECTATE:
				typeText += ChatColor.GRAY; //§7
				break;
			case EXIT:
			case ERROR:
				typeText += ChatColor.RED; //§c
				break;
		}
		typeText += ChatColor.BOLD + "["; //§l
		typeText += type;
		typeText += "]";
		editSign.getSide(Side.FRONT).setLine(0, typeText);
		editSign.update(true);
	}

	public void displayQueue() {
		Sign editSign = (Sign) this.sign.getLocation().getBlock().getState();
		if (track.getHostedRace() != null) {
			editSign.getSide(Side.FRONT).setLine(3, "");
			editSign.update();
			return;
		}
		switch (type) {
			case TRIALS:
				if (track.getQueue().size() > 0) {
					editSign.getSide(Side.FRONT).setLine(3, ChatColor.GOLD + "" + ChatColor.ITALIC + track.getQueue().size() + " in queue"); //"§6§o"
				} else {
					editSign.getSide(Side.FRONT).setLine(3, "");
				}
				break;
			case VERSUS:
				int playerFill = 0;
				if (track.getVersusRace() != null) {
					playerFill = track.getVersusRace().playerSize();
				}
				editSign.getSide(Side.FRONT).setLine(3, playerFill + " / " + track.getVersusSpawns().size());
				break;
			default:
				editSign.getSide(Side.FRONT).setLine(3, "");
				break;
		}
		editSign.update();
	}

	public void displayStatus(TrackStatus status) {
		Sign editSign = (Sign) this.sign.getLocation().getBlock().getState();
		boolean displayStatusAlways = type == RaceSignType.TRIALS || type == RaceSignType.VERSUS;
		boolean shouldOverrideStatusHide = status == TrackStatus.HOSTING && type == RaceSignType.SPECTATE;
		if (!displayStatusAlways && !shouldOverrideStatusHide) {
			editSign.getSide(Side.FRONT).setLine(2, "");
		} else {
			switch (status) {
				case OPEN:
					editSign.getSide(Side.FRONT).setLine(2, ChatColor.GREEN + "" + ChatColor.BOLD + "OPEN"); //§a§l
					break;
				case IN_USE:
					editSign.getSide(Side.FRONT).setLine(2, ChatColor.GRAY + "" + ChatColor.BOLD + "IN USE"); //§7§l
					break;
				case CLOSED:
					editSign.getSide(Side.FRONT).setLine(2, ChatColor.RED + "" + ChatColor.BOLD + "CLOSED"); //§c§l
					break;
				case IN_LOBBY:
					editSign.getSide(Side.FRONT).setLine(2, ChatColor.YELLOW + "" + ChatColor.BOLD + "IN LOBBY"); //§e§l
					break;
				case HOSTING:
					editSign.getSide(Side.FRONT).setLine(2, ChatColor.AQUA + "" + ChatColor.BOLD + "HOSTING"); //§b§l
					break;
			}
		}
		editSign.update();
	}

	public void setType(RaceSignType type) {
		this.type = type;
		this.displayType();
		this.displayStatus(track.getStatus());
	}

	public Sign getSign() {
		return sign;
	}

	public Track getTrack() {
		return track;
	}

	public void onRightClick(Player p) {
		this.track.onRightClick(p, type, laps);
	}

	public void setLaps(int laps) {
		this.laps = laps;
	}
}
