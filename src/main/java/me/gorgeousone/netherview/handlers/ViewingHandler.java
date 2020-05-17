package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.DisplayUtils;
import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustum;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustumFactory;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
	
	public void hideViewSession(Player player) {
		DisplayUtils.removeFakeBlocks(player, getViewSession(player));
		removeVieSession(player);
	}
	
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
		ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum2(playerEyeLoc.toVector(), portal.getPortalRect());
		
		viewedPortals.put(player.getUniqueId(), portal);
		viewedProjections.put(player.getUniqueId(), projection);
		
		Map<BlockVec, BlockData> visibleBlocks = new HashMap<>();
		
		long start = System.currentTimeMillis();
		
		if (displayFrustum) {
			visibleBlocks.putAll(getBlocksInFrustum(projection, playerFrustum));
		}
		
		long stamp = System.currentTimeMillis();
		
		if (hidePortalBlocks) {
			for (Block portalBlock : portal.getPortalBlocks()) {
				visibleBlocks.put(new BlockVec(portalBlock), Material.AIR.createBlockData());
			}
		}
		
		
		displayBlocks(player, visibleBlocks);
		System.out.println("calc time " + (stamp - start));
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
		
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int y = min.getY(); y <= max.getY(); y++) {
				for (int z = min.getZ(); z <= max.getZ(); z++) {
					
					BlockVec corner = new BlockVec(x, y, z);
					
					if (!frustum.contains(corner.toVector()))
						continue;
					
					blocksInFrustum.putAll(projection.getCopiesAround(new BlockVec(x, y, z)));
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockData> updatedCopies) {
		
		for (ProjectionCache projection : portalHandler.getLinkedProjections(cache)) {
			
			Map<BlockVec, BlockData> projectionUpdates = new HashMap<>();
			
			for (BlockVec blockPos : updatedCopies.keySet()) {
				projectionUpdates.put(blockPos, projection.updateCopy(blockPos, updatedCopies.get(blockPos)));
			}
			
			World projectionWorld = projection.getWorld();
			
			//TODO iterate through view-session players
			for (Player player : projectionWorld.getPlayers()) {
				
				if (viewedProjections.get(player.getUniqueId()) != projection)
					continue;
				
				Portal portal = viewedPortals.get(player.getUniqueId());
				ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum2(player.getEyeLocation().toVector(), portal.getPortalRect());
				
				for (BlockVec blockPos : projectionUpdates.keySet()) {
					
					if (playerFrustum.contains(blockPos.toVector()))
						player.sendBlockChange(blockPos.toLocation(projectionWorld), projectionUpdates.get(blockPos));
				}
			}
		}
	}
	
//	public void refreshProjection(Portal portal, BlockData blockCopy) {
//
//		World portalWorld = portal.getWorld();
//
//		new BukkitRunnable() {
//			@Override
//			public void run() {
//
//				for (Player player : portalWorld.getPlayers()) {
//
//					if(!hasViewSession(player) || viewedPortals.get(player.getUniqueId()) != portal)
//						continue;
//
//					if(getViewSession(player).contains(blockCopy)) {
//						displayBlockCopy(player, blockCopy);
//					}
//				}
//			}
//		}.runTask(main);
//	}
	
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
			
			if(viewSession.containsKey(iterator.next()))
				iterator.remove();
		}
		
		viewSession.putAll(blocksToDisplay);
		
		DisplayUtils.removeFakeBlocks(player, removedBlocks);
		DisplayUtils.displayFakeBlocks(player, blocksToDisplay);
	}
	
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