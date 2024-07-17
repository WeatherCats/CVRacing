package org.cubeville.cvracing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import org.cubeville.commons.commands.CommandParser;
import org.cubeville.cvracing.commands.*;
import org.cubeville.cvracing.dbfiles.RacingDB;

import java.io.IOException;

public class CVRacing extends JavaPlugin implements Listener {

    private CommandParser commandParser;
    private RacingDB db;

    public void onEnable() {
        this.getLogger().info("CVRacing Started!");

        // database setup
        this.db = new RacingDB(this);
        try {
            this.db.createBackup(this);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.db.load();

        RaceManager.setPlugin(this);

        // import from config
        ConfigImportManager.importConfiguration(this);

        // import data from db
        ScoreManager.importDataFromDatabase(this.db);
        CustomizationManager.importDataFromDatabase(this.db);

        // load leaderboards
        TrackManager.loadLeaderboards();

        //commands
        commandParser = new CommandParser();
        commandParser.addCommand(new CreateTrack(this));
        commandParser.addCommand(new SetTrialsSpawn(this));
        commandParser.addCommand(new SetTrackExit(this));
        commandParser.addCommand(new SetTrackFinishExit(this));
        commandParser.addCommand(new SetTrackSpectate(this));
        commandParser.addCommand(new SetTrackType(this));
        commandParser.addCommand(new AddRaceSign(this));
        commandParser.addCommand(new OpenTrack(this));
        commandParser.addCommand(new CloseTrack(this));
        commandParser.addCommand(new AddCheckpoint(this));
        commandParser.addCommand(new AddLeaderboard(this));
        commandParser.addCommand(new DeleteCheckpoint(this));
        commandParser.addCommand(new DeleteTrack(this));
        commandParser.addCommand(new DeleteSign(this));
        commandParser.addCommand(new DeleteLeaderboard(this));
        commandParser.addCommand(new AddVersusSpawn(this));
        commandParser.addCommand(new RedefineCheckpointRegion(this));
        commandParser.addCommand(new AddCheckpointRegion(this));
        commandParser.addCommand(new DeleteCheckpointRegion(this));
        commandParser.addCommand(new SetCheckpointRegionReset(this));
        commandParser.addCommand(new SetSignLaps(this));
        commandParser.addCommand(new SetTrackTpToCp(this));
        commandParser.addCommand(new SetTrackSurvival(this));
        commandParser.addCommand(new SetTrackGroundFails(this));
        commandParser.addCommand(new AddCheckpointCommand(this));
        commandParser.addCommand(new DeleteCheckpointCommand(this));
        commandParser.addCommand(new DeleteVersusSpawn(this));
        commandParser.addCommand(new SetTimingMethod(this));
        commandParser.addCommand(new ListCheckpointCommands());
        commandParser.addCommand(new ListVersusSpawns());
        commandParser.addCommand(new ListCheckpoints());
        commandParser.addCommand(new GetPB());
        commandParser.addCommand(new GetLeaderboard());
        commandParser.addCommand(new UseSplits());
        commandParser.addCommand(new GetRank());
        commandParser.addCommand(new ClearPlayerScores());
        commandParser.addCommand(new ClearTrackScores());
        commandParser.addCommand(new DeleteScore());
        commandParser.addCommand(new ListSigns());
        commandParser.addCommand(new ListLeaderboards());
        commandParser.addCommand(new CompareSplits());
        commandParser.addCommand(new Help());
        commandParser.addCommand(new Customize());
        commandParser.addCommand(new HostAddPlayers());
        commandParser.addCommand(new HostEnd());
        commandParser.addCommand(new HostListPlayers());
        commandParser.addCommand(new HostCountdown());
        commandParser.addCommand(new HostStart());
        commandParser.addCommand(new HostTransfer());
        commandParser.addCommand(new HostRemovePlayers());
        commandParser.addCommand(new HostSetLaps());
        commandParser.addCommand(new HostAnnounce());
        commandParser.addCommand(new GiveCustomBoat());
        commandParser.addCommand(new ClearRaceInventory());
        commandParser.addCommand(new CenterPosition());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new EventHandlers(this), this);
    }

    public void onDisable() {
        this.db.disconnect();
        TrackManager.clearArmorStands();
        RaceManager.cancelAllRaces("The plugin has been disabled");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equals("race")) {
            return commandParser.execute(sender, args);
        }
        return false;
    }

}
