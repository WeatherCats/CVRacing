package org.cubeville.cvracing;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvracing.models.*;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.List;
import java.util.UUID;

public class EventHandlers implements Listener {

	JavaPlugin plugin;
	long countdownFreeze;

	public EventHandlers(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() != null &&
			event.getAction() == Action.RIGHT_CLICK_BLOCK &&
			SignManager.signMaterials.contains(event.getClickedBlock().getType())
		) {
			RaceSign sign = SignManager.getSign(event.getClickedBlock().getLocation());
			if (sign == null) {
				return;
			}
			sign.onRightClick(event.getPlayer());
			event.setCancelled(true);
		}

		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK
				|| event.getAction() == Action.RIGHT_CLICK_AIR)
				&& event.getItem() != null) {
			String displayName = event.getItem().getItemMeta().getDisplayName();
			if (displayName.equals(RaceUtilities.getLeaveItem().getItemMeta().getDisplayName())) {
				event.setCancelled(true);
				RaceManager.cancelRace(event.getPlayer(), "You left the race!");
			} else if (displayName.equals(RaceUtilities.getCPResetItem().getItemMeta().getDisplayName())) {
				event.setCancelled(true);
				Race race = RaceManager.getRace(event.getPlayer());
				if (race == null) { return; }
				race.tpPlayerToReset(event.getPlayer());
			} else if (displayName.equals(RaceUtilities.getLeaveQueueItem().getItemMeta().getDisplayName())) {
				event.setCancelled(true);
				Race race = RaceManager.getRace(event.getPlayer());
				if (race instanceof VersusRace) {
					((VersusRace) race).removePlayer(event.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onExit(VehicleExitEvent e) {
		if (e.getExited().getType() == EntityType.PLAYER && RaceManager.racingVehicles.contains(e.getVehicle().getType())) {
			// will cancel race if the player left their vehicle in a race
			Player p = (Player) e.getExited();
			Race r = RaceManager.getRace(p);
			if (r != null && TrackManager.vehicleRaceTypes.contains(r.getTrack().getType())) {
				RaceManager.cancelRace(p, "You left the vehicle during the race.");
				e.getVehicle().remove();
			}
		}
	}

	@EventHandler
	public void onEntityDismount(EntityDismountEvent e) {
		if (e.getEntity() instanceof Player && e.getDismounted() instanceof ArmorStand) {
			Player p = (Player) e.getEntity();
			Race r = RaceManager.getRace(p);
			if (r != null && r.isCountingDown(p)) {
				RaceManager.cancelRace(p, "You tried to get a false start!");
			}
		}
	}

	@EventHandler
	public void onVehicleDestroy(VehicleDestroyEvent e) {
		if (RaceManager.racingVehicles.contains(e.getVehicle().getType()) && e.getVehicle().getPassengers().size() > 0 && e.getVehicle().getPassengers().get(0).getType() == EntityType.PLAYER) {
			// will cancel race if the player exited their vehicle in a race
			Player p = (Player) e.getVehicle().getPassengers().get(0);
			Race r = RaceManager.getRace(p);
			if (r != null && TrackManager.vehicleRaceTypes.contains(r.getTrack().getType())) {
				RaceManager.cancelRace(p, "Your vehicle was destroyed during the race.");
				e.getVehicle().remove();
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (RaceManager.racingVehicles.contains(e.getEntity().getType()) && e.getEntity().getPassengers().size() > 0 && e.getEntity().getPassengers().get(0).getType() == EntityType.PLAYER) {
			Player p = (Player) e.getEntity().getPassengers().get(0);
			Race r = RaceManager.getRace(p);
			if (r != null && TrackManager.vehicleRaceTypes.contains(r.getTrack().getType())) {
				RaceManager.cancelRace(p, "Your vehicle was killed during the race.");
				e.getEntity().remove();
			}
		}
	}


	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		Race r = RaceManager.getRace(p);
		if (r != null) {
			RaceManager.cancelRace(p, "You left the game during the race.");
			if (p.getVehicle() != null) {
				p.getVehicle().remove();
			}
		}

		// remove player from any hosted lobbies
		for (Track track : TrackManager.getTracks()) {
			if (track.getQueue().contains(p)) {
				track.removePlayerFromQueue(p);
				for (RaceSign sign : track.getSigns()) {
					sign.displayQueue();
				}
			}
			if (track.getHostedRace() != null && track.getHostedRace().hasPlayer(p)) {
				RaceManager.removePlayerFromHostedLobby(track, p);
			}
		}
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (e.getEntity().getType() == EntityType.PLAYER) {
			Player p = (Player) e.getEntity();
			if (p.getHealth() - e.getDamage() < 1) {
				Race r = RaceManager.getRace(p);
				if (r != null) {
					RaceManager.cancelRace(p, "You died during the race.");
					if (!r.getTrack().isSurvival()) {
						e.setCancelled(true);
					}
					if (p.getVehicle() != null) {
						p.getVehicle().remove();
					}
					if (!r.getTrack().isSurvival()) {
						p.setInvulnerable(true);
					}
					countdownFreeze = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						() -> p.setInvulnerable(false), 10L);
					if (!r.getTrack().isSurvival()) {
						p.teleport(r.getTrack().getExit());
						p.setHealth(20.0);
					}
				}
			}
		}
	}
}
