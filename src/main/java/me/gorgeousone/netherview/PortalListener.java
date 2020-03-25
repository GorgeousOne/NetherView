package me.gorgeousone.netherview;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalListener implements Listener {
	
	private PortalHandler portalHandler;
	
	public PortalListener(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
	}
	
	@EventHandler
	public void onPortalTravel(PlayerPortalEvent event) {
		
		Location from = event.getFrom();
		Location to = event.getTo();
		
		PortalStructure portal = portalHandler.getRegisteredPortalByBlock();
	}
}
