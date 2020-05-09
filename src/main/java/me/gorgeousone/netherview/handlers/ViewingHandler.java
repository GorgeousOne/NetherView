package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.blockcache.CacheProjection;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustum;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustumFactory;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
	
	private Main main;
	private PortalHandler portalHandler;
	private BlockCacheHandler cacheHandler;
	
	private Map<UUID, CacheProjection> viewedProjections;
	private Map<UUID, Set<BlockCopy>> playerViewSessions;
	
	public ViewingHandler(Main main,
	                      PortalHandler portalHandler,
	                      BlockCacheHandler cacheHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.cacheHandler = cacheHandler;
		
		viewedProjections = new HashMap<>();
		playerViewSessions = new HashMap<>();
	}
	
	public void reset() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (hasViewSession(player))
				hideViewSession(player);
		}
		
		playerViewSessions.clear();
	}
	
	public Set<BlockCopy> getViewSession(Player player) {
		
		UUID uuid = player.getUniqueId();
		
		if (!playerViewSessions.containsKey(uuid))
			playerViewSessions.put(uuid, new HashSet<>());
		
		return playerViewSessions.get(uuid);
	}
	
	public boolean hasViewSession(Player player) {
		return playerViewSessions.containsKey(player.getUniqueId());
	}
	
	public void hideViewSession(Player player) {
		
		if (!hasViewSession(player))
			return;
		
		for (BlockCopy copy : getViewSession(player))
			refreshBlock(player, copy);
		
		playerViewSessions.remove(player.getUniqueId());
	}
	
	public void displayNearestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal portal = portalHandler.getNearestPortal(playerEyeLoc);
		
		if (portal == null) {
			hasViewSession(player);
			return;
		}
		
		Vector portalDistance = portal.getLocation().subtract(playerEyeLoc).toVector();
		
		if (portalDistance.lengthSquared() > main.getPortalDisplayRangeSquared()) {
			hideViewSession(player);
			return;
		}
		
		double distanceToPortalRect;
		AxisAlignedRect portalRect = portal.getPortalRect();
		
		if (portal.getAxis() == Axis.X) {
			distanceToPortalRect = portalRect.getMin().getZ() - playerEyeLoc.getZ();
		} else {
			distanceToPortalRect = portalRect.getMin().getX() - playerEyeLoc.getX();
		}
		
		if (Math.abs(distanceToPortalRect) > 0.5) {
			displayPortalTo(player, playerEyeLoc, portal, true, main.hidePortalBlocks());
			
			//if the player is standing inside the portal, portal blocks should be displayed
		} else if (portalRect.contains(playerEyeLoc.toVector())) {
			hideViewSession(player);
			
			//if the player is standing somewhere next to the portal, portal blocks should still be hidden to avoid light flickering
		} else {
			displayPortalTo(player, playerEyeLoc, portal, false, main.hidePortalBlocks());
		}
	}
	
	public void displayPortalTo(Player player,
	                            Location playerEyeLoc,
	                            Portal portal,
	                            boolean displayFrustum,
	                            boolean hidePortalBlocks) {
		
		
		if (!cacheHandler.isLinked(portal))
			return;
		
		Map.Entry<CacheProjection, CacheProjection> projectingCaches = cacheHandler.getProjectionCaches(portal);
		
		CacheProjection projection = ViewingFrustumFactory.isPlayerBehindPortal(player, portal) ? projectingCaches.getKey() : projectingCaches.getValue();
		ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum2(playerEyeLoc.toVector(), portal.getPortalRect());
		
		viewedProjections.put(player.getUniqueId(), projection);
		
		Set<BlockCopy> visibleBlocks = new HashSet<>();
		
		if (displayFrustum) {
			visibleBlocks.addAll(getBlocksInFrustum(projection, playerFrustum));
//			visibleBlocks.addAll(getAllBlocks(cache));
			displayFrustum(player, playerFrustum);
		}
		
		if (hidePortalBlocks) {
			for (Block block : portal.getPortalBlocks()) {
				BlockCopy air = new BlockCopy(block);
				air.setData(Material.AIR.createBlockData());
				visibleBlocks.add(air);
			}
		}
		
		displayBlocks(player, visibleBlocks);
	}
	
	private Set<BlockCopy> getAllBlocks(CacheProjection cache) {
		
		Set<BlockCopy> allBlocks = new HashSet<>();
		
		BlockVec min = cache.getMin();
		BlockVec max = cache.getMax();
		
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int y = min.getY(); y <= max.getY(); y++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					
					BlockCopy copy = cache.getCopyAt(new BlockVec(x, y, z));
					
					if (copy != null)
						allBlocks.add(copy);
				}
			}
		}
		
		return allBlocks;
	}
	
	private Set<BlockCopy> getBlocksInFrustum(CacheProjection projection, ViewingFrustum frustum) {
		
		Set<BlockCopy> blocksInFrustum = new HashSet<>();
		
		BlockVec min = projection.getMin();
		BlockVec max = projection.getMax();
		
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int y = min.getY(); y <= max.getY(); y++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					
					BlockVec corner = new BlockVec(x, y, z);
					
					if (!frustum.contains(corner.toVector()))
						continue;
					
					blocksInFrustum.addAll(projection.getCopiesAround(new BlockVec(x, y, z)));
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	public void updateProjections(BlockCache cache, Set<BlockCopy> updatedCopies) {
		
		for (CacheProjection projection : cacheHandler.getLinkedProjections(cache)) {
			
			Set<BlockCopy> projectionUpdates = new HashSet<>();
			
			for (BlockCopy updatedCopy : updatedCopies)
				projectionUpdates.add(projection.updateCopy(updatedCopy));
			
			World projectionWorld = projection.getWorld();
			
			for (Player player : projectionWorld.getPlayers()) {
				
				if(viewedProjections.get(player.getUniqueId()) != projection)
					continue;
				
				Set<BlockCopy> viewSession = getViewSession(player);
				
				for (BlockCopy blockCopy : projectionUpdates) {
					
					if(viewSession.contains(blockCopy))
						player.sendBlockChange(blockCopy.getPosition().toLocation(projectionWorld), blockCopy.getBlockData());
				}
			}
		}
	}
	
	private void displayFrustum(Player player, ViewingFrustum frustum) {
		
		AxisAlignedRect nearPlane = frustum.getNearPlaneRect();
		World world = player.getWorld();
		
		player.spawnParticle(Particle.FLAME, nearPlane.getMin().toLocation(world), 0, 0, 0, 0);
		player.spawnParticle(Particle.FLAME, nearPlane.getMax().toLocation(world), 0, 0, 0, 0);
	}
	
	private void displayBlocks(Player player, Set<BlockCopy> blocksToDisplay) {
		
		Set<BlockCopy> viewSession = getViewSession(player);
		Iterator<BlockCopy> iterator = viewSession.iterator();
		
		while (iterator.hasNext()) {
			BlockCopy nextCopy = iterator.next();
			
			if (!blocksToDisplay.contains(nextCopy)) {
				refreshBlock(player, nextCopy);
				iterator.remove();
			}
		}
		
		blocksToDisplay.removeIf(blockCopy -> !viewSession.add(blockCopy));
		
		for (BlockCopy copy : blocksToDisplay) {
			player.sendBlockChange(copy.getPosition().toLocation(player.getWorld()), copy.getBlockData());
		}
	}
	
	private void refreshBlock(Player player, BlockCopy blockCopy) {
		Location blockLoc = blockCopy.getPosition().toLocation(player.getWorld());
		player.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
	}
}
