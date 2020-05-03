package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
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

import javax.sound.sampled.Port;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalHandler {
	
	private Main main;
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, PortalLink> portalLinks;
	private Map<Portal, Map.Entry<BlockCache, BlockCache>> blockCaches;
	
	public PortalHandler(Main main) {
		this.main = main;
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
		Bukkit.broadcastMessage(ChatColor.GRAY + "Portal added: " + portal.getWorld().getName() + ", " + portal.getPortalRect().getMin().toString());
		return portal;
	}
	
	private void addPortal(Portal portal) {
		
		UUID worldID = portal.getWorld().getUID();
		
		if(!worldsWithPortals.containsKey(worldID))
			worldsWithPortals.put(worldID, new HashSet<>());
		
		worldsWithPortals.get(worldID).add(portal);
	}
	
	public void removePortal(Portal portal) {
		
		Bukkit.broadcastMessage("removed portal at " + portal.getLocation().toVector().toString());
		portalLinks.entrySet().removeIf(linkEntry -> linkEntry.getValue().getCounterPortal() == portal);
		portalLinks.remove(portal);
		getPortals(portal.getWorld()).remove(portal);
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
	
	public PortalLink getPortalLink(Portal portal) {
		return portalLinks.get(portal);
	}
	
	public void linkPortal(Portal portal, Portal counterPortal) {
		
		if(!counterPortal.equalsInSize(portal))
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are dissimilar in size, it is difficult to get a clear view...");
		
		if(!blockCaches.containsKey(counterPortal))
			blockCaches.put(counterPortal, BlockCacheFactory.createBlockCache(counterPortal, main.getPortalProjectionDist()));
		
		portalLinks.put(portal, new PortalLink(portal, counterPortal, blockCaches.get(counterPortal)));
	}
}
