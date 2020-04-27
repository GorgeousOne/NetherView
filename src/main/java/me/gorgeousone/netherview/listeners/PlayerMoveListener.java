package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
	
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	public PlayerMoveListener(PortalHandler portalHandler, ViewingHandler viewingHandler) {
		
		this.portalHandler = portalHandler;
		this.viewingHandler = viewingHandler;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		if (event.getPlayer().getGameMode() == GameMode.SPECTATOR)
			return;
		
		if (event.getFrom().toVector().equals(event.getTo().toVector()))
			return;
		
		Player player = event.getPlayer();
		World.Environment worldType = player.getWorld().getEnvironment();
		
		if (worldType != World.Environment.NORMAL && worldType != World.Environment.NETHER)
			return;
		
		Location playerLoc = player.getEyeLocation();
		
		Portal portal = portalHandler.getNearestPortal(playerLoc);
		
		if (portal == null)
			return;
		
		Vector portalDistance = portal.getLocation().subtract(playerLoc).toVector();
		double viewDistanceSquared = 20 * 20;
		
		if (portalDistance.lengthSquared() > viewDistanceSquared)
			return;
		
		viewingHandler.displayPortal(player, portal);
	}
}