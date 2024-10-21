package org.cubeville.cvracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.*;
import org.cubeville.cvracing.RaceSignType;
import org.cubeville.cvracing.TrackManager;
import org.cubeville.cvracing.models.HostedRace;
import org.cubeville.cvracing.models.RaceState;
import org.cubeville.cvracing.models.Track;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ForceJoinCommand extends BaseCommand {
    
    private JavaPlugin plugin;
    
    public ForceJoinCommand(JavaPlugin plugin) {
        super("forcejoin");
        
        addBaseParameter(new CommandParameterString());
        addBaseParameter(new CommandParameterUUID());
        addBaseParameter(new CommandParameterEnum(RaceSignType.class));
        addBaseParameter(new CommandParameterInteger());
        
        setPermission("cvracing.forcejoin");
        this.plugin = plugin;
    }
    
    @Override
    public CommandResponse execute(CommandSender commandSender, Set<String> set, Map<String, Object> map, List<Object> baseParameters) throws CommandExecutionException {
        FileConfiguration config = plugin.getConfig();
        String name = baseParameters.get(0).toString().toLowerCase();
        UUID uuid = (UUID) baseParameters.get(1);
        RaceSignType type = (RaceSignType) baseParameters.get(2);
        Integer laps = (Integer) baseParameters.get(3);
        
        if (!config.contains("tracks." + name)) {
            throw new CommandExecutionException("Track " + baseParameters.get(0) + " does not exist.");
        }
        
        if (Bukkit.getPlayer(uuid) == null) {
            throw new CommandExecutionException("Player with UUID " + uuid + " does not exist or is not online.");
        }
        
        Player affectedPlayer = Bukkit.getPlayer(uuid);
        
        TrackManager.getTrack(name).onRightClick(affectedPlayer, type, laps);
        
        return new CommandResponse("Player joined race!");
    }
}
