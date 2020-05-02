package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLink;
import me.gorgeousone.netherview.portal.PortalLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalHandler {
	
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, PortalLink> portalLinks;
	private Map<Portal, Map.Entry<BlockCache, BlockCache>> blockCaches;
	
	public PortalHandler() {
		worldsWithPortals = new HashMap<>();
		portalLinks = new HashMap<>();
		blockCaches = new HashMap<>();
	}
	
	public void reset() {
		worldsWithPortals.clear();
		portalLinks.clear();
		blockCaches.clear();
	}
	
	/**
	 * Registers new portals and links them to their counter portal
	 */
	public Portal addPortalStructure(Block portalBlock) {
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		addPortal(portal);
		
		blockCaches.put(portal, BlockCacheFactory.createBlockCache(portal, 20));
		Bukkit.broadcastMessage(ChatColor.GRAY + "Portal added: " + portal.getWorld().getName() + ", " + portal.getPortalRect().getMin().toString());
		return portal;
	}
	
	private void addPortal(Portal portal) {
		
		UUID worldID = portal.getWorld().getUID();
		
		if(!worldsWithPortals.containsKey(worldID))
			worldsWithPortals.put(worldID, new HashSet<>());
		
		worldsWithPortals.get(worldID).add(portal);
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public Portal getPortalByBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return null;
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.containsBlock(portalBlock))
				return portal;
		}
		
		return null;
	}
	
	public Portal getNearestPortal(Location playerLoc) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : getPortals(playerLoc.getWorld())) {
			
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
	
//	public BlockCache getBlockCache(Portal portal, boolean isPlayerBehindPortal) {
//
//		Map.Entry<BlockCache, BlockCache> entry = blockCaches.get(portal);
//
//		if(entry != null)
//			return isPlayerBehindPortal ? entry.getValue() : entry.getKey();
//
//		return null;
//	}
	
	public PortalLink getPortalLink(Portal portal) {
		return portalLinks.get(portal);
	}
	
	public void linkPortal(Portal portal, Portal counterPortal) {
		Bukkit.broadcastMessage(ChatColor.GRAY + "linked portal");
		portalLinks.put(portal, new PortalLink(portal, counterPortal, blockCaches.get(counterPortal)));
	}
}
