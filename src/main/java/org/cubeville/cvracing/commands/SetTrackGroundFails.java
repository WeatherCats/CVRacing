package org.cubeville.cvracing.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.*;
import org.cubeville.cvracing.TrackManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SetTrackGroundFails extends BaseCommand {
    JavaPlugin plugin;
    
    public SetTrackGroundFails(JavaPlugin plugin) {
        super("track groundfails");
        addBaseParameter(new CommandParameterString());
        addBaseParameter(new CommandParameterBoolean());
        
        setPermission("cvracing.setup.groundfails");
        this.plugin = plugin;
    }
    
    @Override
    public CommandResponse execute(CommandSender commandSender, Set<String> set, Map<String, Object> map,
                                   List<Object> baseParameters) throws CommandExecutionException {
        FileConfiguration config = plugin.getConfig();
        
        String name = baseParameters.get(0).toString().toLowerCase();
        boolean value = (boolean) baseParameters.get(1);
        
        if (!config.contains("tracks." + name)) {
            throw new CommandExecutionException("Track with name " + baseParameters.get(0) + " does not exist!");
        }
        
        config.set("tracks." + name + ".groundfails", value);
        TrackManager.getTrack(name).setGroundFails(value);
        
        plugin.saveConfig();
        return new CommandResponse("&aYou have successfully set ground fails for the track " + name + " to " + value + "!");
    }
}
