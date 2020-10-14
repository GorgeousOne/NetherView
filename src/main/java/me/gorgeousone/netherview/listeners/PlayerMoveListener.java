package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.utils.InvulnerabilityUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * A listener class that informs the view handler to update the portal view for players when they move.
 * It also takes care of the instant teleportation feature.
 */
public class PlayerMoveListener implements Listener {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final CustomPortalHandler customPortalHandler;
	private final Material portalMaterial;
	
	public PlayerMoveListener(NetherViewPlugin main,
	                          ViewHandler viewHandler,
	                          CustomPortalHandler customPortalHandler,
	                          Material portalMaterial) {
		
		this.main = main;
		this.viewHandler = viewHandler;
		this.customPortalHandler = customPortalHandler;
		this.portalMaterial = portalMaterial;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		boolean enteredNewBlock = playerEnteredNewBlock(from, to);
		
		if (main.canCreatePortalViews(player.getWorld())) {
			
			//sets player temporarily invulnerable so the game will instantly teleport them on entering a portal
			if (main.isInstantTeleportEnabled() && enteredNewBlock && mortalEnteredPortal(player, from, to)) {
				InvulnerabilityUtils.setTemporarilyInvulnerable(player, main, 2);
			}
			
			Vector fromVec = from.toVector();
			Vector toVec = to.toVector();
			
			if (!fromVec.equals(toVec) &&
			    
			    player.getGameMode() != GameMode.SPECTATOR &&
			    viewHandler.hasPortalViewEnabled(player) &&
			    player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
				
				Vector playerMovement = toVec.subtract(fromVec);
				viewHandler.displayClosestPortalTo(player, player.getEyeLocation().add(playerMovement));
			}
		}
	}
	
	/**
	 * Checks if the player in a mortal game mode (so they would usually not be teleported instantly by a nether portal) and if they are entering a nether portal.
	 */
	private boolean mortalEnteredPortal(Player player, Location from, Location to) {
		
		GameMode gameMode = player.getGameMode();
		
		return (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) &&
		       to.getBlock().getType() == portalMaterial &&
		       from.getBlock().getType() != portalMaterial;
	}
	
	private boolean playerEnteredNewBlock(Location from, Location to) {
		return
				(int) from.getX() != (int) to.getX() ||
				(int) from.getY() != (int) to.getY() ||
				(int) from.getZ() != (int) to.getZ();
	}
	
	@EventHandler
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		
		Player player = event.getPlayer();
		
		if (!viewHandler.hasViewSession(player)) {
			return;
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				viewHandler.displayClosestPortalTo(player, player.getEyeLocation());
			}
		}.runTaskLater(main, 2);
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if (event.getNewGameMode() == GameMode.SPECTATOR) {
			viewHandler.hidePortalProjection(event.getPlayer());
		}
	}
}