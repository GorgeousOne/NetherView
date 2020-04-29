package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.handlers.ViewingHandler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
	
	private JavaPlugin plugin;
	private ViewingHandler viewingHandler;
	
	public PlayerMoveListener(JavaPlugin plugin, ViewingHandler viewingHandler) {
		this.plugin = plugin;
		this.viewingHandler = viewingHandler;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.SPECTATOR)
			return;
		
		//TODO switch to checking permitted worlds
		World.Environment worldType = player.getWorld().getEnvironment();
		
		if (worldType != World.Environment.NORMAL && worldType != World.Environment.NETHER)
			return;
		
		if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
			Vector movement = event.getTo().clone().subtract(event.getFrom()).toVector();
			viewingHandler.displayNearestPortalTo(player, player.getEyeLocation().add(movement));
		}
	}
	
	@EventHandler
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		
		Player player = event.getPlayer();
		
		if (player.getGameMode() == GameMode.SPECTATOR)
			return;
		
		World.Environment worldType = player.getWorld().getEnvironment();
		
		if (worldType == World.Environment.NORMAL || worldType == World.Environment.NETHER) {
			new BukkitRunnable() {
				@Override
				public void run() {
					viewingHandler.displayNearestPortalTo(player, player.getLocation());
					
				}
			}.runTaskLater(plugin, 2);
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if(event.getNewGameMode() == GameMode.SPECTATOR)
			viewingHandler.removeViewSession(event.getPlayer());
	}
}