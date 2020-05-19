package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.blocktype.Axis;
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
	
	private NetherView main;
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, Set<Portal>> linkedPortals;
	private Map<BlockCache, Set<ProjectionCache>> linkedProjections;
	
	public PortalHandler(NetherView main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		linkedPortals = new HashMap<>();
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
		
		return portal;
	}
	
	private void addPortal(Portal portal) {
		
		UUID worldID = portal.getWorld().getUID();
		worldsWithPortals.putIfAbsent(worldID, new HashSet<>());
		worldsWithPortals.get(worldID).add(portal);
	}
	
	public void removePortal(Portal portal) {
		
		if (linkedPortals.containsKey(portal)) {
			
			for (Portal linkedPortal : linkedPortals.get(portal))
				linkedPortal.unlink();
			
			linkedProjections.remove(portal.getFrontCache());
			linkedProjections.remove(portal.getBackCache());
		}
		
		if (portal.isLinked()) {
			
			Portal counterPortal = portal.getCounterPortal();
			linkedProjections.get(counterPortal.getFrontCache()).remove(portal.getBackProjection());
			linkedProjections.get(counterPortal.getBackCache()).remove(portal.getFrontProjection());
		}
		
		linkedPortals.remove(portal);
		getPortals(portal.getWorld()).remove(portal);
	}
	
	public Portal getPortalByBlock(Block portalBlock) {
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.getPortalBlocks().contains(portalBlock))
				return portal;
		}
		
		return null;
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public Set<Portal> getLinkedPortals(Portal portal) {
		return linkedPortals.getOrDefault(portal, new HashSet<>());
	}
	
	public Portal getNearestLinkedPortal(Location playerLoc) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : getPortals(playerLoc.getWorld())) {
			
			if (!portal.isLinked())
				continue;
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public Set<BlockCache> getBlockCaches(World world) {
		
		Set<BlockCache> caches = new HashSet<>();
		
		for (Portal portal : getPortals(world)) {
			caches.add(portal.getFrontCache());
			caches.add(portal.getBackCache());
		}
		
		return caches;
	}
	
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal))
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are dissimilar in size, it is difficult to get a clear view...");
		
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		BlockCache cache1 = counterPortal.getFrontCache();
		BlockCache cache2 = counterPortal.getBackCache();
		
		//the projections caches are switching positions because of to the rotation transform
		ProjectionCache projection1 = new ProjectionCache(portal, cache2, linkTransform);
		ProjectionCache projection2 = new ProjectionCache(portal, cache1, linkTransform);
		
		portal.setLinkedTo(counterPortal, new AbstractMap.SimpleEntry<>(projection1, projection2));
		
		linkedPortals.putIfAbsent(counterPortal, new HashSet<>());
		linkedPortals.get(counterPortal).add(portal);
		
		linkedProjections.putIfAbsent(cache1, new HashSet<>());
		linkedProjections.putIfAbsent(cache2, new HashSet<>());
		linkedProjections.get(cache1).add(projection2);
		linkedProjections.get(cache2).add(projection1);
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
