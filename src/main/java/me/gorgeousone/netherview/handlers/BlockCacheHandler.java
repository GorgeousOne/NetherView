package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.blockcache.CacheCopy;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Axis;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockCacheHandler {
	
	private Main main;
	
	private Map<Portal, Portal> counterPortals;
	
	//map of portals with their natural blocks around them
	private Map<Portal, Map.Entry<BlockCache, BlockCache>> blockCaches;
	//map of portals and their copied area of their counter portals
	private Map<Portal, Map.Entry<CacheCopy, CacheCopy>> projectionCaches;
	//map of portals with all the portals, that are going to project their area
	private Map<Portal, Set<Portal>> linkedPortals;
	
	public BlockCacheHandler(Main main) {
		this.main = main;
		
		blockCaches = new HashMap<>();
		projectionCaches = new HashMap<>();
		
		counterPortals = new HashMap<>();
		linkedPortals = new HashMap<>();
	}
	
	public Portal getCounterPortal(Portal portal) {
		return counterPortals.get(portal);
	}
	
	public boolean isLinked(Portal portal) {
		return counterPortals.containsKey(portal);
	}
	
	public void createBlockCaches(Portal portal) {
		blockCaches.put(portal, BlockCacheFactory.createBlockCache(portal, main.getPortalProjectionDist()));
	}
	
	public Map.Entry<BlockCache, BlockCache> getBlockCaches(Portal portal) {
		
		blockCaches.computeIfAbsent(portal, v -> BlockCacheFactory.createBlockCache(portal, main.getPortalProjectionDist()));
		return blockCaches.get(portal);
	}
	
	public boolean hasBlockCaches(Portal portal) {
		return blockCaches.containsKey(portal);
	}
	
	public Map.Entry<CacheCopy, CacheCopy> getProjectionCaches(Portal portal) {
		return projectionCaches.get(portal);
	}
	
	public void linkPortal(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal))
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are dissimilar in size, it is difficult to get a clear view...");

		Map.Entry<BlockCache, BlockCache> blockCaches = getBlockCaches(counterPortal);
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		//notice that the block caches are also switching key value position related to the transform
		projectionCaches.put(portal, new AbstractMap.SimpleEntry<>(
				new CacheCopy(blockCaches.getValue(), linkTransform),
				new CacheCopy(blockCaches.getKey(), linkTransform)
		));
		
		counterPortals.put(portal, counterPortal);
		linkedPortals.computeIfAbsent(counterPortal, v -> new HashSet<>());
		linkedPortals.get(counterPortal).add(portal);
	}
	
	public void deletePortal(Portal portal) {
		
		for (Portal linkedPortal : linkedPortals.get(portal))
			projectionCaches.remove(linkedPortal);
		
		linkedPortals.remove(portal);
		projectionCaches.remove(portal);
		blockCaches.remove(portal);
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
