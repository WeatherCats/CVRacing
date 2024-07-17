package org.cubeville.cvracing.commands;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.*;
import org.cubeville.cvracing.TrackManager;

import java.util.*;

public class SetTrackFinishExit extends Command {
    private JavaPlugin plugin;
    
    public SetTrackFinishExit(JavaPlugin plugin) {
        super("track setfinishexit");
        
        addBaseParameter(new CommandParameterString());
        setPermission("cvracing.setup.finishexit");
        this.plugin = plugin;
    }
    
    @Override
    public CommandResponse execute(Player player, Set<String> set, Map<String, Object> map, List<Object> baseParameters)
        throws CommandExecutionException {
        
        FileConfiguration config = plugin.getConfig();
        
        if (!config.contains("tracks." + baseParameters.get(0))) {
            throw new CommandExecutionException("Track " + baseParameters.get(0) + " does not exist.");
        }
        
        String locationsPath = "tracks." + baseParameters.get(0) + ".finishexit";
        
        Location pLoc = player.getLocation();
        List<String> locParameters = new ArrayList<>(
            Arrays.asList(
                pLoc.getWorld().getName(), // world
                String.valueOf(pLoc.getX()),
                String.valueOf(pLoc.getY()),
                String.valueOf(pLoc.getZ()),
                String.valueOf(pLoc.getYaw()),
                String.valueOf(pLoc.getPitch())
            )
        );
        
        config.set(locationsPath, String.join(",", locParameters));
        TrackManager.getTrack((String) baseParameters.get(0)).setFinishExit(pLoc);
        plugin.saveConfig();
        
        return new CommandResponse("Set player location as finish exit point for the track " + baseParameters.get(0));
    }
}
