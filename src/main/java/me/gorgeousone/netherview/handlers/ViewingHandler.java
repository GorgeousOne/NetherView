package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.ViewingFrustum;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ViewingHandler {
	
	private PortalHandler portalHandler;
	
	private Map<PortalStructure, BlockCache> netherCaches;
	private Map<UUID, Set<BlockCopy>> playerViews;
	
	public ViewingHandler(PortalHandler portalHandler) {
		
		this.portalHandler = portalHandler;
	
		netherCaches = new HashMap<>();
		playerViews = new HashMap<>();
	}
	
	public Set<BlockCopy> getViewSession(Player player) {
		
		UUID uuid = player.getUniqueId();
		
		if(!playerViews.containsKey(uuid))
			playerViews.put(uuid, new HashSet<>());
		
		return playerViews.get(uuid);
	}
	
	public void displayPortal(Player player, PortalStructure portal) {
		
		if (portal.getWorld().getEnvironment() != World.Environment.NORMAL)
			return;
		
		if (!netherCaches.containsKey(portal))
			netherCaches.put(portal, BlockCacheFactory.createBlockCache(portal, portalHandler.getLinkedNetherPortal(portal), 10));
		
		Location playerEyeLoc = player.getEyeLocation();
		AxisAlignedRect nearPlane = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		boolean playerIsRelativelyNegativeToPortal = isPlayerRelativelyNegativeToPortal(playerEyeLoc, portal.getLocation(), portalFacing);
		
		if (playerIsRelativelyNegativeToPortal)
			nearPlane.translate(portalFacing);
		
		ViewingFrustum playerViewingFrustum = new ViewingFrustum(playerEyeLoc.toVector(), nearPlane);
		PortalSide portalSideToDisplay = playerIsRelativelyNegativeToPortal ? PortalSide.POSITIVE : PortalSide.NEGATIVE;
		
		BlockCache netherBlockCache = getCachedBlocks(portal, portalSideToDisplay);
		Set<BlockCopy> visibleBlocks = detectBlocksInView(playerViewingFrustum, netherBlockCache);
		renderNetherBLocks(player, portal, visibleBlocks);
	}
	
	private BlockCache getCachedBlocks(PortalStructure portal, PortalSide portalSideToDisplay) {
		return netherCaches.get(portal);
	}
	
	private boolean isPlayerRelativelyNegativeToPortal(Location playerLoc, Location portalLoc, Vector portalFacing) {
		Vector portalDist = portalLoc.toVector().subtract(playerLoc.toVector());
		return portalFacing.dot(portalDist) > 0;
	}
	
	private ViewingFrustum createViewingFrustum(Location viewpoint, PortalStructure portal) {

		AxisAlignedRect nearPlane = portal.getPortalRect();
		nearPlane.translate(nearPlane.getPlane().getNormal().multiply(0.5));

		return null;
	}
	
	private Set<BlockCopy> detectBlocksInView(ViewingFrustum viewingFrustum, BlockCache netherCache) {
		
		Set<BlockCopy> blocksInCone = new HashSet<>();
		BlockVec min = netherCache.getCopyMin();
		BlockVec max = netherCache.getCopyMax();
		
		for (int x = min.getX(); x < max.getX(); x++) {
			for (int y = min.getY(); y < max.getY(); y++) {
				for (int z = min.getZ(); z < max.getZ(); z++) {
					
					if (viewingFrustum.contains(new Vector(x, y, z)))
						blocksInCone.addAll(netherCache.getCopiesAround(x, y, z));
				}
			}
		}
		
		return blocksInCone;
	}
	
	private void renderNetherBLocks(Player player, PortalStructure portal, Set<BlockCopy> visibleBlocks) {
		
		World playerWorld = player.getWorld();
		Set<BlockCopy> viewSession = getViewSession(player);
		
		for (Block block : portal.getPortalBlocks()) {
			player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		}
		
		Iterator<BlockCopy> iterator = viewSession.iterator();

		while (iterator.hasNext()) {
			BlockCopy nextCopy = iterator.next();

			if (!visibleBlocks.contains(nextCopy)) {
				refreshBlock(player, nextCopy);
				iterator.remove();
			}
		}
		
		for (BlockCopy copy : visibleBlocks) {
			if(viewSession.add(copy))
				player.sendBlockChange(copy.getPosition().toLocation(playerWorld), copy.getBlockData());
		}
	}
	
	private void refreshBlock(Player player, BlockCopy blockCopy) {
		Location blockLoc = blockCopy.getPosition().toLocation(player.getWorld());
		player.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
	}
}
