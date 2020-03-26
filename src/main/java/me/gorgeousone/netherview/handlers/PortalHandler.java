package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.portal.PortalStructure;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

public class PortalHandler {
	
	private Map<PortalStructure, PortalStructure> runningPortals;
	
	public PortalHandler() {
		this.runningPortals = new HashMap<>();
	}
	
	public void addPortals(PortalStructure portal, PortalStructure destination) {
		
		Bukkit.broadcastMessage(ChatColor.GRAY + "connected portals " +
		                        portal.getWorld().getEnvironment().name().toLowerCase() + " " + portal.getLocation().toVector().toString() + " and " +
		                        destination.getWorld().getEnvironment().name().toLowerCase() + " " + destination.getLocation().toVector().toString());
		
		runningPortals.put(portal, destination);
	}
	
	public boolean containsPortalWithBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return false;
		
		for (PortalStructure portal : runningPortals.keySet()) {
			if (portal.containsBlock(portalBlock))
				return true;
		}
		
		return false;
	}
	
	public PortalStructure nearestPortal(Location playerLoc) {
		
		PortalStructure nearestPortal = null;
		double minDist = -1;
		
		for(PortalStructure portal : runningPortals.keySet()) {
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if(nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public PortalStructure getLinkedNetherPortal(PortalStructure portal) {
		return runningPortals.get(portal);
	}
}
