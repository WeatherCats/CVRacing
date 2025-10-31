package org.cubeville.cvracing;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvracing.dbfiles.RacingDB;
import org.cubeville.cvracing.models.CustomizationState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class CustomizationManager {
    private static HashMap<UUID, CustomizationState> customizations = new HashMap<>();
    private static RacingDB database;

    public static void importDataFromDatabase(RacingDB db) {
        database = db;
        try {
            ResultSet customizationSet = db.getAllPlayers();
            if (customizationSet == null) { return; }
            while (customizationSet.next()) {
                UUID playerUUID = UUID.fromString(customizationSet.getString("uuid"));
                CustomizationState cs = new CustomizationState(
                    customizationSet.getString("boat_material"),
                    customizationSet.getString("horse_color"),
                    customizationSet.getString("horse_style"),
                    customizationSet.getString("horse_armor")
                );
                customizations.put(playerUUID, cs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Boat spawnBoat(Player player, Location location) {
        CustomizationState cs = getCustomizationState(player);
        Boat boat = (Boat) Objects.requireNonNull(location.getWorld()).spawnEntity(location, cs.getBoatType());
        return boat;
    }

    public static Horse spawnHorse(Player player, Location location) {
        CustomizationState cs = getCustomizationState(player);
        Horse h = (Horse) player.getWorld().spawnEntity(location, EntityType.HORSE);
        h.getInventory().setSaddle(new ItemStack(Material.SADDLE, 1));
        if (cs.getHorseArmor() != null) {
            h.getInventory().setArmor(cs.getHorseArmor());
        }
        h.setTamed(true);
        h.setOwner(player);
        h.setJumpStrength(.8);
        h.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(.5);
        h.setColor(cs.horseColor);
        h.setStyle(cs.horseStyle);
        return h;
    }

    public static CustomizationState getCustomizationState(Player p) {
        if (!customizations.containsKey(p.getUniqueId())) {
            CustomizationState cs = createCustomizationState(p);
            database.addPlayer(p, cs);
        }
        return customizations.get(p.getUniqueId());
    }

    public static CustomizationState createCustomizationState(Player p) {
        CustomizationState cs = new CustomizationState();
        customizations.put(p.getUniqueId(), cs);
        return cs;
    }

    public static void updatePlayerEntry(Player p) {
        database.updatePlayer(p, getCustomizationState(p));
    }

}
