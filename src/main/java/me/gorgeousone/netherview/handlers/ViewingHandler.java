package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.DisplayUtils;
import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustum;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustumFactory;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ViewingHandler {
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	private Map<UUID, Portal> viewedPortals;
	private Map<UUID, ProjectionCache> viewedProjections;
	private Map<UUID, Map<BlockVec, BlockData>> playerViewSessions;
	
	public ViewingHandler(NetherView main, PortalHandler portalHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		
		viewedProjections = new HashMap<>();
		playerViewSessions = new HashMap<>();
		viewedPortals = new HashMap<>();
	}
	
	public void reset() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (hasViewSession(player))
				hideViewSession(player);
		}
		
		playerViewSessions.clear();
	}
	
	public Map<BlockVec, BlockData> getViewSession(Player player) {
		
		UUID uuid = player.getUniqueId();
		
		playerViewSessions.putIfAbsent(uuid, new HashMap<>());
		return playerViewSessions.get(uuid);
	}
	
	public boolean hasViewSession(Player player) {
		return playerViewSessions.containsKey(player.getUniqueId());
	}
	
	/**
	 * Removes the players view session and removes all sent fake blocks.
	 *
	 * @param player
	 */
	public void hideViewSession(Player player) {
		DisplayUtils.removeFakeBlocks(player, getViewSession(player));
		removeVieSession(player);
	}
	
	/**
	 * Simply removes the player from the system.
	 */
	public void removeVieSession(Player player) {
		playerViewSessions.remove(player.getUniqueId());
		viewedPortals.remove(player.getUniqueId());
	}
	
	public void displayNearestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal portal = portalHandler.getNearestLinkedPortal(playerEyeLoc);
		
		if (portal == null) {
			hideViewSession(player);
			removeVieSession(player);
			return;
		}
		
		Vector portalDistance = portal.getLocation().subtract(playerEyeLoc).toVector();
		
		if (portalDistance.lengthSquared() > main.getPortalDisplayRangeSquared()) {
			hideViewSession(player);
			removeVieSession(player);
			return;
		}
		
		AxisAlignedRect portalRect = portal.getPortalRect();
		
		//display the portal totally normal if the player is not standing next to or in the portal
		if (getDistanceToPortal(playerEyeLoc, portalRect) > 0.5) {
			displayPortalTo(player, playerEyeLoc, portal, true, main.hidePortalBlocks());
			
			//keep portal blocks hidden (if requested) if the player is standing next to the portal to avoid light flickering
		} else if (!portalRect.contains(playerEyeLoc.toVector())) {
			displayPortalTo(player, playerEyeLoc, portal, false, main.hidePortalBlocks());
			
			//if the player is standing inside the portal projection should be dropped
		} else {
			hideViewSession(player);
			removeVieSession(player);
		}
	}
	
	private double getDistanceToPortal(Location playerEyeLoc, AxisAlignedRect portalRect) {
		
		double distanceToPortal;
		
		if (portalRect.getAxis() == Axis.X) {
			distanceToPortal = portalRect.getMin().getZ() - playerEyeLoc.getZ();
		} else {
			distanceToPortal = portalRect.getMin().getX() - playerEyeLoc.getX();
		}
		
		return Math.abs(distanceToPortal);
	}
	
	public void displayPortalTo(Player player,
	                            Location playerEyeLoc,
	                            Portal portal,
	                            boolean displayFrustum,
	                            boolean hidePortalBlocks) {
		
		if (!portal.isLinked())
			return;
		
		ProjectionCache projection = ViewingFrustumFactory.isPlayerBehindPortal(player, portal) ? portal.getFrontProjection() : portal.getBackProjection();
		ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum(playerEyeLoc.toVector(), portal.getPortalRect(), projection.getCacheLength());
		
		viewedPortals.put(player.getUniqueId(), portal);
		viewedProjections.put(player.getUniqueId(), projection);
		
		Map<BlockVec, BlockData> visibleBlocks = new HashMap<>();
		
		if (displayFrustum) {
			visibleBlocks.putAll(getBlocksInFrustum(projection, playerFrustum));
		}
		
		if (hidePortalBlocks) {
			for (Block portalBlock : portal.getPortalBlocks()) {
				visibleBlocks.put(new BlockVec(portalBlock), Material.AIR.createBlockData());
			}
		}
		
		displayBlocks(player, visibleBlocks);
	}
	
	//	private Map<BlockVec, BlockData> getAllBlocks(ProjectionCache cache) {
	//
	//		Map<BlockVec, BlockData> allBlocks = new HashSet<>();
	//
	//		BlockVec min = cache.getMin();
	//		BlockVec max = cache.getMax();
	//
	//		for (int x = min.getX(); x <= max.getX(); x++) {
	//			for (int y = min.getY(); y <= max.getY(); y++) {
	//				for (int z = min.getZ(); z <= max.getZ(); z++) {
	//
	//					BlockData copy = cache.getCopyAt(new BlockVec(x, y, z));
	//
	//					if (copy != null)
	//						allBlocks.add(copy);
	//				}
	//			}
	//		}
	//
	//		return allBlocks;
	//	}
	
	private Map<BlockVec, BlockData> getBlocksInFrustum(ProjectionCache projection, ViewingFrustum frustum) {
		
		Map<BlockVec, BlockData> blocksInFrustum = new HashMap<>();
		
		BlockVec min = projection.getMin();
		BlockVec max = projection.getMax();
		
		AxisAlignedRect nearPlaneRect = frustum.getNearPlaneRect();
		AxisAlignedRect farPlaneRect = frustum.getFarPlaneRect();
		
		if (farPlaneRect.getAxis() == Axis.X) {
			
			double newMinX = Math.min(nearPlaneRect.getMin().getX(), farPlaneRect.getMin().getX());
			double newMaxX = Math.max(nearPlaneRect.getMax().getX(), farPlaneRect.getMax().getX());
			
			if (newMinX > min.getX())
				min.setX((int) Math.floor(newMinX));
			if (newMaxX < max.getX())
				max.setX((int) Math.ceil(newMaxX));
			
		} else {
			
			double newMinZ = Math.min(nearPlaneRect.getMin().getZ(), farPlaneRect.getMin().getZ());
			double newMaxZ = Math.max(nearPlaneRect.getMax().getZ(), farPlaneRect.getMax().getZ());
			
			if (newMinZ > min.getZ())
				min.setZ((int) Math.floor(newMinZ));
			if (newMaxZ < max.getZ())
				max.setZ((int) Math.ceil(newMaxZ));
		}
		
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int y = min.getY(); y <= max.getY(); y++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					
					if (frustum.contains(blockPos.toVector()))
						blocksInFrustum.putAll(projection.getCopiesAround(new BlockVec(x, y, z)));
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	/**
	 * Forwards the changes made in a block cache to all the linked projections. This also live-updates what the players see
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockData> updatedCopies) {
		
		for (ProjectionCache projection : portalHandler.getLinkedProjections(cache)) {
			
			Map<BlockVec, BlockData> projectionUpdates = new HashMap<>();
			
			for (Map.Entry<BlockVec, BlockData> entry : updatedCopies.entrySet()) {
				
				Transform blockTransform = projection.getTransform();
				BlockVec projectionBlockPos = blockTransform.transformVec(entry.getKey().clone());
				BlockData projectionBlockData = blockTransform.rotateData(entry.getValue().clone());
				
				projection.updateCopy(projectionBlockPos, projectionBlockData);
				projectionUpdates.put(projectionBlockPos, projectionBlockData);
			}
			
			for (UUID playerID : viewedProjections.keySet()) {
				
				if (viewedProjections.get(playerID) != projection)
					continue;
				
				Portal portal = viewedPortals.get(playerID);
				Player player = Bukkit.getPlayer(playerID);
				
				ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum(
						player.getEyeLocation().toVector(),
						portal.getPortalRect(),
						projection.getCacheLength());
				
				Map<BlockVec, BlockData> blocksInFrustum = new HashMap<>();
				Map<BlockVec, BlockData> viewSession = getViewSession(player);
				
				for (Map.Entry<BlockVec, BlockData> entry : projectionUpdates.entrySet()) {
					
					BlockVec blockPos = entry.getKey();
					BlockData blockData = entry.getValue();
					
					if (playerFrustum.containsBlock(blockPos.toVector())) {
						blocksInFrustum.put(blockPos, blockData);
						viewSession.put(blockPos, blockData);
					}
				}
				
				DisplayUtils.displayFakeBlocks(player, blocksInFrustum);
			}
		}
	}
	
	private void displayBlocks(Player player, Map<BlockVec, BlockData> blocksToDisplay) {
		
		Map<BlockVec, BlockData> viewSession = getViewSession(player);
		
		Map<BlockVec, BlockData> removedBlocks = new HashMap<>();
		Iterator<BlockVec> iterator = viewSession.keySet().iterator();
		
		while (iterator.hasNext()) {
			
			BlockVec blockPos = iterator.next();
			
			if (!blocksToDisplay.containsKey(blockPos)) {
				removedBlocks.put(blockPos, viewSession.get(blockPos));
				iterator.remove();
			}
		}
		
		iterator = blocksToDisplay.keySet().iterator();
		
		while (iterator.hasNext()) {
			
			if (viewSession.containsKey(iterator.next()))
				iterator.remove();
		}
		
		viewSession.putAll(blocksToDisplay);
		DisplayUtils.removeFakeBlocks(player, removedBlocks);
		DisplayUtils.displayFakeBlocks(player, blocksToDisplay);
	}
	
	//	private void displayFrustum(Player player, ViewingFrustum frustum) {
	//
	//		AxisAlignedRect nearPlane = frustum.getNearPlaneRect();
	//		AxisAlignedRect farPlane = frustum.getFarPlaneRect();
	//		World world = player.getWorld();
	//
	//		player.getWorld().spawnParticle(Particle.FLAME, nearPlane.getMin().toLocation(world), 0, 0, 0, 0);
	//		player.getWorld().spawnParticle(Particle.FLAME, nearPlane.getMax().toLocation(world), 0, 0, 0, 0);
	//
	//		player.getWorld().spawnParticle(Particle.FLAME, farPlane.getMin().toLocation(world), 0, 0, 0, 0);
	//		player.getWorld().spawnParticle(Particle.FLAME, farPlane.getMax().toLocation(world), 0, 0, 0, 0);
	//	}
	
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getLinkedPortals(portal);
		affectedPortals.add(portal);
		
		Iterator<Map.Entry<UUID, Portal>> iter = viewedPortals.entrySet().iterator();
		
		while (iter.hasNext()) {
			
			Map.Entry<UUID, Portal> playerView = iter.next();
			
			if (!affectedPortals.contains(playerView.getValue()))
				continue;
			
			iter.remove();
			hideViewSession(Bukkit.getPlayer(playerView.getKey()));
		}
	}
}