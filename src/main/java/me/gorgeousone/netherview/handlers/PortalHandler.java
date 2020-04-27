package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLink;
import me.gorgeousone.netherview.portal.PortalLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortalHandler {
	
	private Set<Portal> portals;
	private Map<Portal, PortalLink> portalLinks;
	private Map<Portal, BlockCache> blockCaches;
	
	public PortalHandler() {
		portals = new HashSet<>();
		portalLinks = new HashMap<>();
		blockCaches = new HashMap<>();
	}
	
	/**
	 * Registers new portals and links them to their counter portal
	 */
	public Portal addPortal(Block portalBlock) {
		
		try {
			Portal portal = PortalLocator.locatePortalStructure(portalBlock);
			portals.add(portal);
			blockCaches.put(portal, BlockCacheFactory.createBlockCache(portal, 6));
			Bukkit.broadcastMessage(ChatColor.GRAY + "New portal: " + portal.getWorld().getName() + " - " + portal.getPortalRect().getMin().toString());
			return portal;
			
		}catch (IllegalArgumentException ignored) {
			ignored.printStackTrace();
			return null;
		}

//		Portal startPortal = getPortalByBlock(from);
//
//		if(startPortal == null) {
//			try {
//				startPortal = PortalLocator.locatePortalStructure(from);
//				portals.add(startPortal);
//				blockCaches.put(startPortal, BlockCacheFactory.createBlockCache(startPortal, 6));
//				portalLinks.put(startPortal, new PortalLink(startPortal, counterPortal));
//				Bukkit.broadcastMessage(ChatColor.GRAY + "Link portal: " + startPortal.getWorld().getName() + " - " + startPortal.getPortalRect().getMin().toString());
//
//			}catch (IllegalArgumentException ignored) {
//				ignored.printStackTrace();
//			}
//		}
	}
	
	public Portal getPortalByBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return null;
		
		for (Portal portal : portals) {
			if (portal.containsBlock(portalBlock))
				return portal;
		}
		
		return null;
	}
	
	public Portal getNearestPortal(Location playerLoc) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : portals) {
			
			if(portal.getWorld() != playerLoc.getWorld())
				continue;
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public BlockCache getBlockCache(Portal portal) {
		return blockCaches.get(portal);
	}
	
	public PortalLink getPortalLink(Portal portal) {
		return portalLinks.get(portal);
	}
	
	public void linkPortal(Portal portal, Portal counterPortal) {
		portalLinks.put(portal, new PortalLink(portal, counterPortal));
	}
}
