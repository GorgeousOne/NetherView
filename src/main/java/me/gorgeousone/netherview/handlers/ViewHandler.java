package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustumFactory;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
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

/**
 * Handler class for running and updating the portal animations for players.
 */
public class ViewHandler {
	
	private NetherView main;
	private PortalHandler portalHandler;
	private PacketHandler packetHandler;
	
	private Map<UUID, Portal> viewedPortals;
	private Map<UUID, ProjectionCache> viewedProjections;
	private Map<UUID, Map<BlockVec, BlockType>> playerViewSessions;
	
	public ViewHandler(NetherView main,
	                   PortalHandler portalHandler,
	                   PacketHandler packetHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.packetHandler = packetHandler;
		
		viewedProjections = new HashMap<>();
		playerViewSessions = new HashMap<>();
		viewedPortals = new HashMap<>();
	}
	
	public void reset() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (isViewingAPortal(player)) {
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
	
	public Portal getViewedPortal(Player player) {
		return viewedPortals.get(player.getUniqueId());
	}
	
	public ProjectionCache getViewedProjection(Player player) {
		return viewedProjections.get(player.getUniqueId());
	}
	
	public boolean isViewingAPortal(Player player) {
		return viewedPortals.containsKey(player.getUniqueId());
	}
	
	/**
	 * Removes the players view session and removes all sent fake blocks.
	 */
	public void hideViewSession(Player player) {
		packetHandler.removeFakeBlocks(player, getViewSession(player));
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
	 * Locates the nearest portal to a player and displays a portal animation to them (if in view range) with fake blocks .
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
			displayPortalTo(player, playerEyeLoc, portal, true, main.hidePortalBlocksEnabled());
			
			//keep portal blocks hidden (if requested) if the player is standing next to the portal to avoid light flickering when moving around the portal
		} else if (!portalRect.contains(playerEyeLoc.toVector())) {
			displayPortalTo(player, playerEyeLoc, portal, false, main.hidePortalBlocksEnabled());
			
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
	
	private void displayPortalTo(Player player,
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
			
			for (Block portalBlock : portal.getPortalBlocks()) {
				visibleBlocks.put(new BlockVec(portalBlock), BlockType.of(Material.AIR));
			}
		}
		
		displayBlocks(player, visibleBlocks);
	}
	
	/**
	 * Forwards the changes made in a block cache to all the linked projection caches. This also live-updates what the players see.
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockType> updatedBlocks) {
		
		for (ProjectionCache projection : portalHandler.getProjectionsLinkedTo(cache)) {
			
			Map<BlockVec, BlockType> projectionUpdates = updateProjection(projection, updatedBlocks);
			
			//TODO stop iterating same players for each projection?
			for (UUID playerID : viewedProjections.keySet()) {
				
				if (viewedProjections.get(playerID) != projection) {
					continue;
				}
				
				Player player = Bukkit.getPlayer(playerID);
				Portal portal = viewedPortals.get(playerID);
				
				ViewFrustum playerFrustum = ViewFrustumFactory.createFrustum(
						player.getEyeLocation().toVector(),
						portal.getPortalRect(),
						projection.getCacheLength());
				
				if (playerFrustum == null) {
					continue;
				}
				
				Map<BlockVec, BlockType> newBlocksInFrustum = getBlocksInFrustum(playerFrustum, projectionUpdates);
				getViewSession(player).putAll(newBlocksInFrustum);
				packetHandler.displayFakeBlocks(player, newBlocksInFrustum);
			}
		}
	}
	
	private Map<BlockVec, BlockType> updateProjection(ProjectionCache projection,
	                                                  Map<BlockVec, BlockType> updatedBlocks) {
		
		Map<BlockVec, BlockType> projectionUpdates = new HashMap();
		
		for (Map.Entry<BlockVec, BlockType> entry : updatedBlocks.entrySet()) {
			
			Transform blockTransform = projection.getLinkTransform();
			BlockVec projectionBlockPos = blockTransform.transformVec(entry.getKey());
			BlockType sourceBlockType = entry.getValue();
			
			if (sourceBlockType == null) {
				projection.removeBlockDataAt(projectionBlockPos);
				continue;
			}
			
			BlockType projectionBlockType = sourceBlockType.rotate(blockTransform.getQuarterTurns());
			
			projection.setBlockTypeAt(projectionBlockPos, projectionBlockType);
			projectionUpdates.put(projectionBlockPos, projectionBlockType);
		}
		
		return projectionUpdates;
	}
	
	private Map<BlockVec, BlockType> getBlocksInFrustum(ViewFrustum playerFrustum,
	                                                    Map<BlockVec, BlockType> projectionUpdates) {
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : projectionUpdates.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockType blockType = entry.getValue();
			
			if (blockType != null && playerFrustum.containsBlock(blockPos.toVector())) {
				blocksInFrustum.put(blockPos, blockType);
			}
		}
		
		return blocksInFrustum;
	}
	
	/**
	 * Adding new blocks to the portal animation for a player.
	 * But first redundant blocks are filtered out and outdated blocks are refreshed for the player.
	 */
	private void displayBlocks(Player player, Map<BlockVec, BlockType> newBlocksToDisplay) {
		
		Map<BlockVec, BlockType> viewSession = getViewSession(player);
		Map<BlockVec, BlockType> removedBlocks = new HashMap<>();
		
		Iterator<BlockVec> viewSessionIter = viewSession.keySet().iterator();
		
		while (viewSessionIter.hasNext()) {
			
			BlockVec blockPos = viewSessionIter.next();
			
			if (!newBlocksToDisplay.containsKey(blockPos)) {
				removedBlocks.put(blockPos, viewSession.get(blockPos));
				viewSessionIter.remove();
			}
		}
		
		newBlocksToDisplay.keySet().removeIf(viewSession::containsKey);
		viewSession.putAll(newBlocksToDisplay);
		
		packetHandler.removeFakeBlocks(player, removedBlocks);
		packetHandler.displayFakeBlocks(player, newBlocksToDisplay);
	}
	
	/**
	 * Removes all to a portal related animations.
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getPortalsLinkedTo(portal);
		affectedPortals.add(portal);
		Iterator<Map.Entry<UUID, Portal>> iter = viewedPortals.entrySet().iterator();
		
		while (iter.hasNext()) {
			
			Map.Entry<UUID, Portal> playerView = iter.next();
			
			//call iter.remove() first because otherwise hideViewSession() will create CurrentModificationException
			if (affectedPortals.contains(playerView.getValue())) {
				iter.remove();
				hideViewSession(Bukkit.getPlayer(playerView.getKey()));
			}
		}
	}
}