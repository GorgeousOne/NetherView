package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustumFactory;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapper.Axis;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler class for running and updating the portal projections for players.
 */
public class ViewHandler {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final PacketHandler packetHandler;
	
	private final Map<UUID, Boolean> portalViewEnabled;
	private final Map<UUID, PlayerViewSession> viewSessions;
	
	public ViewHandler(NetherViewPlugin main,
	                   PortalHandler portalHandler,
	                   PacketHandler packetHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.packetHandler = packetHandler;
		
		portalViewEnabled = new HashMap<>();
		viewSessions = new HashMap<>();
	}
	
	public void reload() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			hidePortalProjection(player);
		}
		
		viewSessions.clear();
	}
	
	/**
	 * Returns true if the player has portal viewing enabled with the /togglenetherview command.
	 */
	public boolean hasPortalViewEnabled(Player player) {
		
		if (!player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			return false;
		}
		
		UUID playerId = player.getUniqueId();
		portalViewEnabled.putIfAbsent(playerId, true);
		return portalViewEnabled.get(player.getUniqueId());
	}
	
	/**
	 * Sets whether the player wants to see portal projections or not if they have the permission to do so.
	 */
	public void setPortalViewEnabled(Player player, boolean viewingEnabled) {
		
		if (player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			portalViewEnabled.put(player.getUniqueId(), viewingEnabled);
			
			if (!viewingEnabled) {
				hidePortalProjection(player);
			}
		}
	}
	
	public Collection<PlayerViewSession> getViewSessions() {
		return viewSessions.values();
	}
	
	public PlayerViewSession getViewSession(Player player) {
		return viewSessions.get(player.getUniqueId());
	}
	
	public PlayerViewSession getOrCreateViewSession(Player player) {
		
		viewSessions.putIfAbsent(player.getUniqueId(), new PlayerViewSession(player));
		return viewSessions.get(player.getUniqueId());
	}
	
	/**
	 * Returns true if currently a portal projection is being displayed to the player.
	 */
	public boolean hasViewSession(Player player) {
		return viewSessions.containsKey(player.getUniqueId());
	}
	
	/**
	 * Removes the player's view session and removes all sent fake blocks.
	 */
	public void hidePortalProjection(Player player) {
		
		if (!hasViewSession(player)) {
			return;
		}
		
		PlayerViewSession session = getViewSession(player);
		
		packetHandler.removeFakeBlocks(player, session.getProjectedBlocks());
		packetHandler.showEntities(player, session.getHiddenEntities());
		packetHandler.hideEntities(player, session.getProjectedEntities().keySet());
		
		unregisterPortalProjection(player);
	}
	
	public void projectEntity(Player player, Entity entity, Transform transform) {
		
		getViewSession(player).getProjectedEntities().put(entity, entity.getLocation());
		packetHandler.showEntity(player, entity, transform);
	}
	
	public void destroyProjectedEntity(Player player, Entity entity) {
		
		getViewSession(player).getProjectedEntities().remove(entity);
		packetHandler.hideEntities(player, Collections.singleton(entity));
	}
	
	public void showEntity(Player player, Entity entity) {
		
		getViewSession(player).getHiddenEntities().remove(entity);
		packetHandler.showEntity(player, entity, new Transform());
	}
	
	public void hideEntity(Player player, Entity entity) {
		
		getViewSession(player).getHiddenEntities().add(entity);
		packetHandler.hideEntities(player, Collections.singleton(entity));
	}
	
	/**
	 * Removes any portal view related data of the player
	 */
	public void unregisterPlayer(Player player) {
		
		portalViewEnabled.remove(player.getUniqueId());
		unregisterPortalProjection(player);
	}
	
	/**
	 * Removes the player's portal projection from the system.
	 */
	public void unregisterPortalProjection(Player player) {
		viewSessions.remove(player.getUniqueId());
	}
	
	/**
	 * Locates the nearest portal to a player and displays a portal projection to them (if in view range) with fake block packets.
	 */
	public void displayClosestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal closestPortal = portalHandler.getClosestPortal(playerEyeLoc, true);
		
		if (portalHandler.portalDoesNotExist(closestPortal)) {
			hidePortalProjection(player);
			return;
		}
		
		Vector portalDistance = closestPortal.getLocation().subtract(playerEyeLoc).toVector();
		
		if (portalDistance.lengthSquared() > main.getPortalDisplayRangeSquared()) {
			hidePortalProjection(player);
			return;
		}
		
		Portal lastViewedPortal = getOrCreateViewSession(player).getViewedPortal();
		
		if (lastViewedPortal != null && !lastViewedPortal.equals(closestPortal)) {
			hidePortalProjection(player);
		}
		
		AxisAlignedRect portalRect = closestPortal.getPortalRect();
		
		//display the portal totally normal if the player is not standing next to or in the portal
		if (getDistanceToPortal(playerEyeLoc, portalRect) > 0.5) {
			displayPortalTo(player, playerEyeLoc, closestPortal, true, main.hidePortalBlocksEnabled());
			
			//keep portal blocks hidden (if ever hidden before) if the player is standing next to the portal to avoid light flickering when moving around the portal
		} else if (!portalRect.contains(playerEyeLoc.toVector())) {
			displayPortalTo(player, playerEyeLoc, closestPortal, false, main.hidePortalBlocksEnabled());
			
			//if the player is standing inside the portal projecting should be dropped
		} else {
			hidePortalProjection(player);
		}
	}
	
	/**
	 * Returns the distance of the location to the rectangle on the axis orthogonal to the axis of the rectangle.
	 */
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
		
		PlayerViewSession session = getOrCreateViewSession(player);
		session.setViewedPortal(portal);
		session.setViewedPortalSide(projection);
		
		if (!displayFrustum) {
			
			session.setLastViewFrustum(null);
			packetHandler.showEntities(player, session.getHiddenEntities());
			packetHandler.hideEntities(player, session.getProjectedEntities().keySet());
			session.getHiddenEntities().clear();
			session.getProjectedEntities().clear();
			
		} else {
			session.setLastViewFrustum(playerFrustum);
		}
		
		displayProjectionBlocks(player, portal, projection, playerFrustum, displayFrustum, hidePortalBlocks);
	}
	
	/**
	 * Collects and displays fake blocks for player to view the portal projection.
	 */
	private void displayProjectionBlocks(Player player,
	                                     Portal portal,
	                                     ProjectionCache projection,
	                                     ViewFrustum playerFrustum,
	                                     boolean displayFrustum,
	                                     boolean hidePortalBlocks) {
		
		Map<BlockVec, BlockType> visibleBlocks = new HashMap<>();
		
		if (playerFrustum != null && displayFrustum) {
			visibleBlocks = playerFrustum.getContainedBlocks(projection);
		}
		
		if (hidePortalBlocks) {
			
			for (Block portalBlock : portal.getPortalBlocks()) {
				visibleBlocks.put(new BlockVec(portalBlock), BlockType.of(Material.AIR));
			}
		}
		
		updateDisplayedBlocks(player, visibleBlocks);
	}
	
	/**
	 * -
	 * Forwards the changes made in a block cache to all the linked projection caches. This also live-updates what players see.
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockType> updatedBlocks) {
		
		Map<ProjectionCache, Set<PlayerViewSession>> sortedSessions = getSessionsSortedByPortalSides();
		
		for (ProjectionCache projection : portalHandler.getProjectionsLinkedTo(cache)) {
			
			Map<BlockVec, BlockType> projectionUpdates = updateProjection(projection, updatedBlocks);
			
			if (!sortedSessions.containsKey(projection)) {
				continue;
			}
			
			for (PlayerViewSession session : sortedSessions.get(projection)) {
				
				ViewFrustum playerFrustum = session.getLastViewFrustum();
				
				if (playerFrustum == null) {
					continue;
				}
				
				Map<BlockVec, BlockType> newBlocksInFrustum = getBlocksInFrustum(playerFrustum, projectionUpdates);
				Player player = session.getPlayer();
				
				getViewSession(player).getProjectedBlocks().putAll(newBlocksInFrustum);
				packetHandler.displayFakeBlocks(player, newBlocksInFrustum);
			}
		}
	}
	
	private Map<BlockVec, BlockType> updateProjection(ProjectionCache projection,
	                                                  Map<BlockVec, BlockType> updatedBlocks) {
		
		Map<BlockVec, BlockType> projectionUpdates = new HashMap<>();
		
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
	
	/**
	 * Returns a map of all the blocks in a block cache that are visible with the player's view frustum through the portal frame.
	 */
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
	
	public Map<ProjectionCache, Set<PlayerViewSession>> getSessionsSortedByPortalSides() {
		
		Map<ProjectionCache, Set<PlayerViewSession>> sortedViewers = new HashMap<>();
		
		for (PlayerViewSession session : viewSessions.values()) {
			
			ProjectionCache projection = session.getViewedPortalSide();
			sortedViewers.putIfAbsent(projection, new HashSet<>());
			sortedViewers.get(projection).add(session);
		}
		
		return sortedViewers;
	}
	
	/**
	 * Adding new blocks to the portal animation for a player.
	 * But first redundant blocks are filtered out and outdated blocks are refreshed for the player.
	 */
	private void updateDisplayedBlocks(Player player, Map<BlockVec, BlockType> newBlocksToDisplay) {
		
		Map<BlockVec, BlockType> lastDisplayedBlocks = getViewSession(player).getProjectedBlocks();
		Map<BlockVec, BlockType> removedBlocks = new HashMap<>();
		
		Iterator<BlockVec> blockIter = lastDisplayedBlocks.keySet().iterator();
		
		while (blockIter.hasNext()) {
			
			BlockVec blockPos = blockIter.next();
			
			if (!newBlocksToDisplay.containsKey(blockPos)) {
				removedBlocks.put(blockPos, lastDisplayedBlocks.get(blockPos));
				blockIter.remove();
			}
		}
		
		newBlocksToDisplay.keySet().removeIf(lastDisplayedBlocks::containsKey);
		lastDisplayedBlocks.putAll(newBlocksToDisplay);
		
		packetHandler.removeFakeBlocks(player, removedBlocks);
		packetHandler.displayFakeBlocks(player, newBlocksToDisplay);
	}
	
	/**
	 * Stops all portal projections that are from this portal or from portals connected to it.
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getPortalsLinkedTo(portal);
		affectedPortals.add(portal);
		
		HashMap<UUID, PlayerViewSession> viewPortalCopy = new HashMap<>(viewSessions);
		
		for (PlayerViewSession session : viewPortalCopy.values()) {
			
			if (affectedPortals.contains(session.getViewedPortal())) {
				hidePortalProjection(session.getPlayer());
			}
		}
	}
}