package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalHandler {
	
	private Main main;
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<BlockCache, Set<ProjectionCache>> linkedProjections;
	
	public PortalHandler(Main main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		linkedProjections = new HashMap<>();
	}
	
	public void reset() {
		worldsWithPortals.clear();
	}
	
	/**
	 * Registers new portals and links them to their counter portal
	 */
	public Portal addPortalStructure(Block portalBlock) {
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		portal.setBlockCaches(BlockCacheFactory.createBlockCaches(portal, main.getPortalProjectionDist()));
		addPortal(portal);
		
		Bukkit.broadcastMessage(ChatColor.GRAY + "Portal added: " + portal.getWorld().getName() + ", " + portal.getPortalRect().getMin().toString());
		return portal;
	}
	
	private void addPortal(Portal portal) {
		
		UUID worldID = portal.getWorld().getUID();
		
		if (!worldsWithPortals.containsKey(worldID))
			worldsWithPortals.put(worldID, new HashSet<>());
		
		worldsWithPortals.get(worldID).add(portal);
	}
	
	public void removePortal(Portal portal) {
		
		Bukkit.broadcastMessage("removed portal at " + portal.getLocation().toVector().toString());
		getPortals(portal.getWorld()).remove(portal);
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public Portal getPortalByBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return null;
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.getPortalBlocks().contains(portalBlock))
				return portal;
		}
		
		return null;
	}
	
	public Portal getNearestPortal(Location playerLoc) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : getPortals(playerLoc.getWorld())) {
			
			if (portal.getWorld() != playerLoc.getWorld())
				continue;
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal))
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are dissimilar in size, it is difficult to get a clear view...");
		
		Map.Entry<BlockCache, BlockCache> blockCaches = counterPortal.getBlockCaches();
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		BlockCache cache1 = blockCaches.getKey();
		BlockCache cache2 = blockCaches.getValue();
		
		World portalWorld = portal.getWorld();
		
		ProjectionCache copy1 = new ProjectionCache(cache2, linkTransform, portalWorld);
		ProjectionCache copy2 = new ProjectionCache(cache1, linkTransform, portalWorld);
		
		//notice that the block caches are also switching key value position related to the transform
		portal.setLinkedTo(counterPortal, new AbstractMap.SimpleEntry<>(copy1, copy2));
		
		linkedProjections.putIfAbsent(cache1, new HashSet<>());
		linkedProjections.putIfAbsent(cache2, new HashSet<>());
		linkedProjections.get(cache1).add(copy2);
		linkedProjections.get(cache2).add(copy1);
	}
	
	public Set<ProjectionCache> getLinkedProjections(BlockCache cache) {
		return linkedProjections.getOrDefault(cache, new HashSet<>());
	}
	
	private Transform calculateLinkTransform(Portal portal, Portal counterPortal) {
		
		Transform linkTransform;
		Vector distance = portal.getLocation().toVector().subtract(counterPortal.getLocation().toVector());
		
		linkTransform = new Transform();
		linkTransform.setTranslation(new BlockVec(distance));
		linkTransform.setRotCenter(new BlockVec(counterPortal.getPortalRect().getMin()));
		
		//during the rotation some weird shifts happen
		//I did not figure out where they come from, for now the translations are a good workaround
		if (portal.getAxis() == counterPortal.getAxis()) {
			
			linkTransform.setRotY180Deg();
			int portalBlockWidth = (int) portal.getPortalRect().width() - 1;
			
			if (counterPortal.getAxis() == Axis.X)
				linkTransform.translate(new BlockVec(portalBlockWidth, 0, 0));
			else
				linkTransform.translate(new BlockVec(0, 0, portalBlockWidth));
			
		} else if (counterPortal.getAxis() == Axis.X) {
			linkTransform.setRotY90DegRight();
			linkTransform.translate(new BlockVec(0, 0, 1));
			
		} else {
			linkTransform.setRotY90DegLeft();
			linkTransform.translate(new BlockVec(1, 0, 0));
		}
		
		return linkTransform;
	}
}
