package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.portal.PortalStructureFactory;
import me.gorgeousone.netherview.handlers.PortalHandler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class TeleportListener implements Listener {
	
	private PortalHandler portalHandler;
	
	public TeleportListener(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerPortalEvent event) {
		
		Location from = event.getFrom();
		
		if (from.getWorld().getEnvironment() != World.Environment.NORMAL)
			return;
		
		Block portalBlock = from.getBlock();
		
		if (!portalHandler.isPartOfPortal(from.getBlock())) {
			
			try {
				portalHandler.addPortal(PortalStructureFactory.locatePortalStructure(portalBlock));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
