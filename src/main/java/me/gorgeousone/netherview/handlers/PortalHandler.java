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
	
	public void linkPortals(PortalStructure overworldPortal, PortalStructure netherPortal) {
		
		Bukkit.broadcastMessage(ChatColor.GRAY + "connected portals " +
		                        overworldPortal.getWorld().getEnvironment().name().toLowerCase() + " " + overworldPortal.getLocation().toVector().toString() + " and " +
		                        netherPortal.getWorld().getEnvironment().name().toLowerCase() + " " + netherPortal.getLocation().toVector().toString());
		
		runningPortals.put(overworldPortal, netherPortal);
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
		
		for (PortalStructure portal : runningPortals.keySet()) {
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
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
