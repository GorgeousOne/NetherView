package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
	
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	
	public PlayerMoveListener(PortalHandler portalHandler, ViewHandler viewHandler) {
		
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
		
		
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		Location playerLoc = player.getEyeLocation();
		
		PortalStructure portal = portalHandler.nearestPortal(playerLoc);
		
		if (portal == null)
			return;
		
		Vector portalDistance = portal.getLocation().subtract(playerLoc).toVector();
		
		double viewDistanceSquared = 50 * 50;
		
		if (portalDistance.lengthSquared() > viewDistanceSquared)
			return;
		
		viewHandler.displayPortal(player, portal);
	}
}
