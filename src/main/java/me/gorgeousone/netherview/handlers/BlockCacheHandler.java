package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.blockcache.CacheProjection;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Axis;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockCacheHandler {
	
	private Main main;
	
	private Map<Portal, Portal> counterPortals;
	
	private Map<Portal, Map.Entry<BlockCache, BlockCache>> sourceCaches;
	private Map<Portal, Map.Entry<CacheProjection, CacheProjection>> projectionCaches;
	private Map<Portal, Set<Portal>> linkedPortals;
	private Map<BlockCache, Set<CacheProjection>> linkedProjections;
	
	public BlockCacheHandler(Main main) {
		this.main = main;
		
		sourceCaches = new HashMap<>();
		projectionCaches = new HashMap<>();
		
		counterPortals = new HashMap<>();
		linkedPortals = new HashMap<>();
		linkedProjections = new HashMap<>();
	}
	
	public Set<BlockCache> getSourceCaches() {
		
		Set<BlockCache> caches = new HashSet<>();
		
		for (Map.Entry<BlockCache, BlockCache> cache : sourceCaches.values()) {
			caches.add(cache.getKey());
			caches.add(cache.getValue());
		}
		
		return caches;
	}
	
	public boolean isLinked(Portal portal) {
		return counterPortals.containsKey(portal);
	}
	
	public Portal getCounterPortal(Portal portal) {
		return counterPortals.get(portal);
	}
	
	public Map.Entry<BlockCache, BlockCache> getBlockCaches(Portal portal) {
		
		sourceCaches.putIfAbsent(portal, BlockCacheFactory.createBlockCaches(portal, main.getPortalProjectionDist()));
		return sourceCaches.get(portal);
	}
	
	//	public void createBlockCaches(Portal portal) {
	//		sourceCaches.put(portal, BlockCacheFactory.createBlockCaches(portal, main.getPortalProjectionDist()));
	//	}
	
	//	public boolean hasBlockCaches(Portal portal) {
	//		return sourceCaches.containsKey(portal);
	//	}
	
	public Map.Entry<CacheProjection, CacheProjection> getProjectionCaches(Portal portal) {
		return projectionCaches.get(portal);
	}
	
	public Set<CacheProjection> getLinkedProjections(BlockCache cache) {
		return linkedProjections.getOrDefault(cache, new HashSet<>());
	}
	
	public void linkPortal(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal))
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are dissimilar in size, it is difficult to get a clear view...");
		
		Map.Entry<BlockCache, BlockCache> blockCaches = getBlockCaches(counterPortal);
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		BlockCache cache1 = blockCaches.getKey();
		BlockCache cache2 = blockCaches.getValue();
		
		World portalWorld = portal.getWorld();
		
		CacheProjection copy1 = new CacheProjection(cache2, linkTransform, portalWorld);
		CacheProjection copy2 = new CacheProjection(cache1, linkTransform, portalWorld);
		
		//notice that the block caches are also switching key value position related to the transform
		projectionCaches.put(portal, new AbstractMap.SimpleEntry<>(copy1, copy2));
		counterPortals.put(portal, counterPortal);
		
		linkedPortals.putIfAbsent(counterPortal, new HashSet<>());
		linkedPortals.get(counterPortal).add(portal);
		
		linkedProjections.putIfAbsent(cache1, new HashSet<>());
		linkedProjections.putIfAbsent(cache2, new HashSet<>());
		linkedProjections.get(cache1).add(copy2);
		linkedProjections.get(cache2).add(copy1);
	}
	
	public void deletePortal(Portal portal) {
		
		for (Portal linkedPortal : linkedPortals.get(portal))
			projectionCaches.remove(linkedPortal);
		
		linkedPortals.remove(portal);
		projectionCaches.remove(portal);
		sourceCaches.remove(portal);
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
