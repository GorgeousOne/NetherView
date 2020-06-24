package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.utils.InvulnerabilityReflection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
	private ViewHandler viewHandler;
	private Material portalMaterial;
	
	public PlayerMoveListener(NetherView main, ViewHandler viewHandler, Material portalMaterial) {
		
		this.main = main;
		this.viewHandler = viewHandler;
		this.portalMaterial = portalMaterial;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		//sets player temporarily invulnerable so the game will instantly teleport them on entering a portal
		if (main.isInstantTeleportEnabled() && mortalEnteredPortal(player, from, to)) {
			InvulnerabilityReflection.setTemporarilyInvulnerable(player, main, 2);
		}
		
		if (player.getGameMode() == GameMode.SPECTATOR || !player.hasPermission(NetherView.VIEW_PERM)) {
			return;
		}
		
		World playerWorld = player.getWorld();
		
		if (playerWorld.getEnvironment() == World.Environment.THE_END || !main.canCreatePortalViews(playerWorld)) {
			return;
		}
		
		Vector fromVec = from.toVector();
		Vector toVec = to.toVector();
		
		if (!fromVec.equals(toVec)) {
			
			Vector playerMovement = toVec.subtract(fromVec);
			viewHandler.displayNearestPortalTo(player, player.getEyeLocation().add(playerMovement));
		}
	}
	
	/**
	 * Checks if the player in a mortal game mode (so they would usually not be teleported instantly by a nether portal) and if they are entering a nether portal.
	 */
	private boolean mortalEnteredPortal(Player player, Location from, Location to) {
		
		GameMode gameMode = player.getGameMode();
		return
				(gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) &&
				playerMovedIntoNewBlock(from, to) &&
				to.getBlock().getType() == portalMaterial &&
				from.getBlock().getType() != portalMaterial;
	}
	
	private boolean playerMovedIntoNewBlock(Location from, Location to) {
		return
				from.getBlockX() != to.getBlockX() ||
				from.getBlockY() != to.getBlockY() ||
				from.getBlockZ() != to.getBlockZ();
	}
	
	@EventHandler
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		
		Player player = event.getPlayer();
		
		if (viewHandler.isViewingAPortal(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					viewHandler.displayNearestPortalTo(player, player.getEyeLocation());
				}
			}.runTaskLater(main, 2);
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if (event.getPlayer().hasPermission(NetherView.VIEW_PERM) && event.getNewGameMode() == GameMode.SPECTATOR) {
			viewHandler.hideViewSession(event.getPlayer());
		}
	}
}