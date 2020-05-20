package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
	
	private NetherView main;
	private ViewingHandler viewingHandler;
	
	public PlayerMoveListener(NetherView main, ViewingHandler viewingHandler) {
		this.main = main;
		this.viewingHandler = viewingHandler;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		
		if (!player.hasPermission(NetherView.VIEW_PERM) || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		
		World playerWorld = player.getWorld();
		
		if (playerWorld.getEnvironment() == World.Environment.THE_END || !main.canCreatePortalsViews(playerWorld)) {
			return;
		}
		
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (!from.toVector().equals(to.toVector())) {
			Vector movement = to.clone().subtract(from).toVector();
			viewingHandler.displayNearestPortalTo(player, player.getEyeLocation().add(movement));
		}
	}
	
	@EventHandler
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		
		Player player = event.getPlayer();
		
		if (player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		
		World.Environment worldType = player.getWorld().getEnvironment();
		
		if (worldType == World.Environment.NORMAL || worldType == World.Environment.NETHER) {
			new BukkitRunnable() {
				@Override
				public void run() {
					viewingHandler.displayNearestPortalTo(player, player.getEyeLocation());
				}
			}.runTaskLater(main, 2);
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if (event.getNewGameMode() == GameMode.SPECTATOR) {
			viewingHandler.hideViewSession(event.getPlayer());
		}
	}
}