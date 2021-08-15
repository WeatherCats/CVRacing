package org.cubeville.cvboatracing.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.*;
import org.cubeville.cvboatracing.TrackManager;

import java.util.*;

public class AddCheckpoint extends Command {

	private JavaPlugin plugin;

	public AddCheckpoint(JavaPlugin plugin) {
		super("track checkpoint add");

		addBaseParameter(new CommandParameterString());
		setPermission("cvboatrace.checkpoints.add");
		this.plugin = plugin;
	}

	@Override
	public CommandResponse execute(Player player, Set<String> set, Map<String, Object> map, List<Object> baseParameters)
		throws CommandExecutionException {

		FileConfiguration config = plugin.getConfig();
		String name = baseParameters.get(0).toString().toLowerCase();

		if (!config.contains("tracks." + name)) {
			throw new CommandExecutionException("Track " + baseParameters.get(0) + " does not exist.");
		}

		Block block = player.getTargetBlock(null, 100);
		if (block.getType() != Material.TRIPWIRE_HOOK) {
			throw new CommandExecutionException("You need to be looking at a tripwire hook to set a checkpoint.");
		}
		Location twLoc = block.getLocation();

		String locationsPath = "tracks." + name + ".checkpoints";

		List<String> twLocations = config.getStringList(locationsPath);
		List<String> locParameters = new ArrayList<>(
			Arrays.asList(
				twLoc.getWorld().getName(), // world
				String.valueOf(twLoc.getBlockX()),
				String.valueOf(twLoc.getBlockY()),
				String.valueOf(twLoc.getBlockZ()),
				String.valueOf(twLoc.getYaw()),
				String.valueOf(twLoc.getPitch())
			)
		);
		twLocations.add(String.join(",", locParameters));
		config.set(locationsPath, twLocations);

		TrackManager.getTrack(name).addCheckpoint(twLoc);
		plugin.saveConfig();

		return new CommandResponse("Successfully created a checkpoint for the track " + baseParameters.get(0) + "!");
	}
}