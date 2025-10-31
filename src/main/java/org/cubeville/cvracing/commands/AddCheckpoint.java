package org.cubeville.cvracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.*;
import org.cubeville.commons.utils.BlockUtils;
import org.cubeville.cvracing.RaceUtilities;
import org.cubeville.cvracing.TrackManager;
import org.cubeville.cvracing.models.CPRegion;
import org.cubeville.cvracing.models.Checkpoint;
import org.cubeville.cvracing.models.Track;

import java.util.*;

public class AddCheckpoint extends Command {

	private JavaPlugin plugin;

	public AddCheckpoint(JavaPlugin plugin) {
		super("track checkpoints add");

		addBaseParameter(new CommandParameterString());
		setPermission("cvracing.setup.cps.edit");
		this.plugin = plugin;
	}

	@Override
	public CommandResponse execute(Player player, Set<String> set, Map<String, Object> map, List<Object> baseParameters)
		throws CommandExecutionException {

		FileConfiguration config = plugin.getConfig();
		String name = baseParameters.get(0).toString().toLowerCase();
		Track track = TrackManager.getTrack(name);

		if (!config.contains("tracks." + name)) {
			throw new CommandExecutionException("Track " + baseParameters.get(0) + " does not exist.");
		}

		Location min, max;

		try {
			min = BlockUtils.getWESelectionMin(player);
			max = BlockUtils.getWESelectionMax(player).add(1.0, 1.0, 1.0);
		}
		catch(IllegalArgumentException e) {
			throw new CommandExecutionException("Please make a cuboid worldedit selection before running this command.");
		}

		Checkpoint cp = new Checkpoint();
		CPRegion cpRegion = cp.addRegion(min, max);

		String locationsPath = "tracks." + name + ".checkpoints." + track.getCheckpoints().size() + "." + cpRegion.getString();

		config.createSection(locationsPath);

		TrackManager.getTrack(name).addCheckpoint(cp);
		plugin.saveConfig();

		return new CommandResponse("Successfully created a checkpoint for the track " + baseParameters.get(0) + "!");
	}
}
