package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.DisplayUtils;
import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.threedstuff.viewfrustum.ViewFrustumFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ViewHandler {
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	private Map<UUID, Portal> viewedPortals;
	private Map<UUID, ProjectionCache> viewedProjections;
	private Map<UUID, Map<BlockVec, BlockType>> playerViewSessions;
	
	public ViewHandler(NetherView main, PortalHandler portalHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		
		viewedProjections = new HashMap<>();
		playerViewSessions = new HashMap<>();
		viewedPortals = new HashMap<>();
	}
	
	public void reset() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (hasViewSession(player)) {
				hideViewSession(player);
			}
		}
		
		viewedPortals.clear();
		viewedProjections.clear();
		playerViewSessions.clear();
	}
	
	/**
	 * Returns a Map of BlockTypes linked to their location that are currently displayed with fake blocks
	 * to a player.
	 */
	public Map<BlockVec, BlockType> getViewSession(Player player) {
		
		UUID uuid = player.getUniqueId();
		
		playerViewSessions.putIfAbsent(uuid, new HashMap<>());
		return playerViewSessions.get(uuid);
	}
	
	public boolean hasViewSession(Player player) {
		return playerViewSessions.containsKey(player.getUniqueId());
	}
	
	/**
	 * Removes the players view session and removes all sent fake blocks.
	 */
	public void hideViewSession(Player player) {
		DisplayUtils.removeFakeBlocks(player, getViewSession(player));
		removeVieSession(player);
	}
	
	/**
	 * Only removes the player reference.
	 */
	public void removeVieSession(Player player) {
		playerViewSessions.remove(player.getUniqueId());
		viewedPortals.remove(player.getUniqueId());
	}
	
	/**
	 * Locates the nearest portal to a player and displays a portal animation to them with fake blocks (if in view range).
	 */
	public void displayNearestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal portal = portalHandler.getNearestPortal(playerEyeLoc, true);
		
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
		
		if (!portal.isLinked()) {
			return;
		}
		
		if (!portal.projectionsAreLoaded()) {
			portalHandler.loadProjectionCachesOf(portal);
		}
		
		portalHandler.updateExpirationTime(portal);
		portalHandler.updateExpirationTime(portal.getCounterPortal());
		
		ProjectionCache projection = ViewFrustumFactory.isPlayerBehindPortal(player, portal) ? portal.getFrontProjection() : portal.getBackProjection();
		ViewFrustum playerFrustum = ViewFrustumFactory.createFrustum(playerEyeLoc.toVector(), portal.getPortalRect(), projection.getCacheLength());
		
		viewedPortals.put(player.getUniqueId(), portal);
		viewedProjections.put(player.getUniqueId(), projection);
		
		Map<BlockVec, BlockType> visibleBlocks = new HashMap<>();
		
		if (playerFrustum != null && displayFrustum) {
			visibleBlocks = playerFrustum.getContainedBlocks(projection);
		}
		
		if (hidePortalBlocks) {
			for (Block portalBlock : portal.getPortalBlocks())
				visibleBlocks.put(new BlockVec(portalBlock), BlockType.of(Material.AIR));
		}
		
		displayBlocks(player, visibleBlocks);
	}
	
	/**
	 * Forwards the changes made in a block cache to all the linked projection caches. This also live-updates what the players see
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockType> updatedCopies) {
		
		for (ProjectionCache projection : portalHandler.getProjectionsLinkedTo(cache)) {
			
			Map<BlockVec, BlockType> projectionUpdates = new HashMap<>();
			
			for (Map.Entry<BlockVec, BlockType> entry : updatedCopies.entrySet()) {
				
				Transform blockTransform = projection.getTransform();
				BlockVec projectionBlockPos = blockTransform.transformVec(entry.getKey().clone());
				BlockType projectionBlockType = entry.getValue().clone().rotate(blockTransform.getQuarterTurns());
				
				projection.setBlockTypeAt(projectionBlockPos, projectionBlockType);
				projectionUpdates.put(projectionBlockPos, projectionBlockType);
			}
			
			//TODO stop iterating same players for each projection?
			for (UUID playerID : viewedProjections.keySet()) {
				
				if (viewedProjections.get(playerID) != projection) {
					continue;
				}
				
				Portal portal = viewedPortals.get(playerID);
				Player player = Bukkit.getPlayer(playerID);
				
				ViewFrustum playerFrustum = ViewFrustumFactory.createFrustum(
						player.getEyeLocation().toVector(),
						portal.getPortalRect(),
						projection.getCacheLength());
				
				if (playerFrustum == null) {
					continue;
				}
				
				Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
				Map<BlockVec, BlockType> viewSession = getViewSession(player);
				
				for (Map.Entry<BlockVec, BlockType> entry : projectionUpdates.entrySet()) {
					
					BlockVec blockPos = entry.getKey();
					BlockType blockType = entry.getValue();
					
					if (blockType == null) {
						blockType = BlockType.of(blockPos.toBlock(player.getWorld()));
					}
					
					if (playerFrustum.containsBlock(blockPos.toVector())) {
						blocksInFrustum.put(blockPos, blockType);
						viewSession.put(blockPos, blockType);
					}
				}
				
				DisplayUtils.displayFakeBlocks(player, blocksInFrustum);
			}
		}
	}
	
	/**
	 * Adding new blocks to the portal animation for a player.
	 * But first redundant blocks are filtered out and outdated blocks are refreshed for the player.
	 */
	private void displayBlocks(Player player, Map<BlockVec, BlockType> blocksToDisplay) {
		
		Map<BlockVec, BlockType> viewSession = getViewSession(player);
		
		Map<BlockVec, BlockType> removedBlocks = new HashMap<>();
		Iterator<BlockVec> iterator = viewSession.keySet().iterator();
		
		while (iterator.hasNext()) {
			
			BlockVec blockPos = iterator.next();
			
			if (!blocksToDisplay.containsKey(blockPos)) {
				removedBlocks.put(blockPos, viewSession.get(blockPos));
				iterator.remove();
			}
		}
		
//		iterator = blocksToDisplay.keySet().iterator();
//
//		while (iterator.hasNext()) {
//
//			if (viewSession.containsKey(iterator.next())) {
//				iterator.remove();
//			}
//		}
		
		viewSession.putAll(blocksToDisplay);
		DisplayUtils.removeFakeBlocks(player, removedBlocks);
		DisplayUtils.displayFakeBlocks(player, blocksToDisplay);
	}
	
	/**
	 * Removes a portal and related portal animations.
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getPortalsLinkedTo(portal);
		affectedPortals.add(portal);
		Iterator<Map.Entry<UUID, Portal>> iter = viewedPortals.entrySet().iterator();
		
		while (iter.hasNext()) {
			
			Map.Entry<UUID, Portal> playerView = iter.next();
			
			//call iter.remove() first because otherwise hideViewSession() will create ConcurrentModificationException
			if (affectedPortals.contains(playerView.getValue())) {
				iter.remove();
				hideViewSession(Bukkit.getPlayer(playerView.getKey()));
			}
		}
	}
}