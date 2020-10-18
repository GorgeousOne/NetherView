package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.utils.TeleportUtils;
import org.bukkit.ChatColor;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A listener class that informs the view handler to update the portal view for players when they move.
 * It also takes care of the instant teleportation feature.
 */
public class PlayerMoveListener implements Listener {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	private final ViewHandler viewHandler;
	private final CustomPortalHandler customPortalHandler;
	private final Material portalMaterial;
	
	private final Set<UUID> teleportedPlayers;
	
	public PlayerMoveListener(NetherViewPlugin plugin,
	                          ConfigSettings configSettings, ViewHandler viewHandler,
	                          CustomPortalHandler customPortalHandler,
	                          Material portalMaterial) {
		
		this.plugin = plugin;
		this.configSettings = configSettings;
		this.viewHandler = viewHandler;
		this.customPortalHandler = customPortalHandler;
		this.portalMaterial = portalMaterial;
		
		teleportedPlayers = new HashSet<>();
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		World world = player.getWorld();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		boolean enteredNewBlock = playerEnteredNewBlock(from, to);
		
		if (configSettings.canCreatePortalViews(world)) {
			
			//sets player temporarily invulnerable so the game will instantly teleport them on entering a portal
			if (configSettings.isInstantTeleportEnabled() && enteredNewBlock && mortalEnteredPortal(player, from, to)) {
				TeleportUtils.setTemporarilyInvulnerable(player, plugin, 2);
			}
			
			handlePortalViewingOnMove(player, from, to);
		}
		
		if (enteredNewBlock && configSettings.canCreatePortalViews(world)) {
			handleCustomPortalTp(player, from, to);
		}
	}
	
	private void handlePortalViewingOnMove(Player player, Location from, Location to) {
		
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
	
	/**
	 * Teleports player if they entered a block of custom portal and it's not already their destination portal
	 */
	private void handleCustomPortalTp(Player player, Location from, Location to) {
		
		CustomPortal portal = customPortalHandler.getPortalAt(to);
		UUID playerId = player.getUniqueId();
		
		if (portal == null) {
			teleportedPlayers.remove(playerId);
			return;
		}
		
		if (teleportedPlayers.contains(playerId) || !portal.isLinked()) {
			return;
		}
		
		Transform tpTransform = portal.getTpTransform();
		
		Vector vel = to.toVector().subtract(from.toVector());
		Vector transformedVel = tpTransform.rotateVec(vel);
		Location destination = tpTransform.transformLoc(to.clone());
		
		player.teleport(destination);
		player.setVelocity(transformedVel);
		teleportedPlayers.add(playerId);
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
		}.runTaskLater(plugin, 2);
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if (event.getNewGameMode() == GameMode.SPECTATOR) {
			viewHandler.hidePortalProjection(event.getPlayer());
		}
	}
}