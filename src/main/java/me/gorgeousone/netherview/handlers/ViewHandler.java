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
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import me.gorgeousone.netherview.wrapping.boundingbox.BoundingBoxUtils;
import me.gorgeousone.netherview.wrapping.boundingbox.EntityBoundingBox;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler class for running and updating the portal animations for players.
 */
public class ViewHandler {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	
	private final PacketHandler packetHandler;
	
	private final Map<UUID, Boolean> portalViewEnabled;
	private final Map<UUID, Portal> viewedPortals;
	private final Map<UUID, ProjectionCache> viewedPortalSides;
	private final Map<UUID, Map<BlockVec, BlockType>> projectedBlocks;
	
	private final HashMap<UUID, Set<Entity>> nearbyEntities;
	private final HashMap<UUID, Set<Entity>> hiddenEntities;
	private final HashMap<Entity, EntityBoundingBox> entityBoundingBoxes;
	
	public ViewHandler(NetherViewPlugin main,
	                   PortalHandler portalHandler,
	                   PacketHandler packetHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.packetHandler = packetHandler;
		
		portalViewEnabled = new HashMap<>();
		viewedPortalSides = new HashMap<>();
		projectedBlocks = new HashMap<>();
		viewedPortals = new HashMap<>();
		
		nearbyEntities = new HashMap<>();
		hiddenEntities = new HashMap<>();
		entityBoundingBoxes = new HashMap<>();
	}
	
	public void reset() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			hidePortalProjection(player);
		}
		
		viewedPortals.clear();
		viewedPortalSides.clear();
		projectedBlocks.clear();
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
	
	/**
	 * Returns true if currently a portal projection is being displayed to the player.
	 */
	public boolean isViewingAPortal(Player player) {
		return viewedPortals.containsKey(player.getUniqueId());
	}
	
	public Portal getViewedPortal(Player player) {
		return viewedPortals.get(player.getUniqueId());
	}
	
	/**
	 * Returns a Map of BlockTypes linked to their location that are currently displayed with fake blocks to the player.
	 * The method is being used very frequently so it does not check for the player's permission to view portal projections.
	 * Returns (and internally adds) an empty Map if no projected blocks found.
	 */
	public Map<BlockVec, BlockType> getProjectedBlocks(Player player) {
		
		UUID uuid = player.getUniqueId();
		projectedBlocks.putIfAbsent(uuid, new HashMap<>());
		return projectedBlocks.get(uuid);
	}
	
	/**
	 * Returns the one of the two projection caches of a portal that is being displayed to the player in the portal view.
	 */
	public ProjectionCache getViewedPortalSide(Player player) {
		return viewedPortalSides.get(player.getUniqueId());
	}
	
	/**
	 * Removes the player's view session and removes all sent fake blocks.
	 */
	public void hidePortalProjection(Player player) {
		
		if (!isViewingAPortal(player)) {
			return;
		}
	
		player.sendMessage("lights out!");
		packetHandler.removeFakeBlocks(player, getProjectedBlocks(player));
		packetHandler.showEntities(player, getHiddenEntities(player));
		unregisterPortalProjection(player);
	}
	
	public Set<Entity> getNearbyEntities(Player player) {
		
		UUID playerId = player.getUniqueId();
		nearbyEntities.putIfAbsent(playerId, new HashSet<>());
		return nearbyEntities.get(playerId);
	}
	
	public void addNearbyEntity(Player player, Entity entity) {
		
		player.sendMessage(ChatColor.GRAY + "adding entity " + entity.getEntityId());
		UUID playerId = player.getUniqueId();
		nearbyEntities.putIfAbsent(playerId, new HashSet<>());
		nearbyEntities.get(playerId).add(entity);
	}
	
	public void removeNearbyEntity(Player player, int entityId) {
		
		player.sendMessage(ChatColor.GRAY + "removing entity " + entityId);
		
		Iterator<Entity> entityIter = getNearbyEntities(player).iterator();
		
		while (entityIter.hasNext()) {
			
			Entity entity = entityIter.next();
			
			if (!entity.isValid() || entity.getEntityId() == entityId) {
				entityBoundingBoxes.remove(entity);
				entityIter.remove();
			}
		}
	}
	
	public EntityBoundingBox getBoundingBox(Entity entity) {
		
		if (!entityBoundingBoxes.containsKey(entity)) {
			entityBoundingBoxes.put(entity, BoundingBoxUtils.getWrappedBoxOf(entity));
		}
		
		return entityBoundingBoxes.get(entity);
	}
	
	public Set<Entity> getHiddenEntities(Player player) {
		
		UUID playerId = player.getUniqueId();
		hiddenEntities.putIfAbsent(playerId, new HashSet<>());
		return hiddenEntities.get(playerId);
	}
	
	/**
	 * Removes any portal view related data of the player
	 */
	public void unregisterPlayer(Player player) {
		
		UUID playerId = player.getUniqueId();
		portalViewEnabled.remove(playerId);
		nearbyEntities.remove(playerId);
		
		if (isViewingAPortal(player)) {
			unregisterPortalProjection(player);
		}
	}
	
	/**
	 * Removes the player's portal projection from the system.
	 */
	public void unregisterPortalProjection(Player player) {
		
		UUID playerId = player.getUniqueId();
		projectedBlocks.remove(playerId);
		hiddenEntities.remove(playerId);
		viewedPortals.remove(playerId);
		viewedPortalSides.remove(playerId);
	}
	
	/**
	 * Locates the nearest portal to a player and displays a portal animation to them (if in view range) with fake blocks .
	 */
	public void displayClosestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal closestPortal = portalHandler.getClosestPortal(playerEyeLoc, true);
		
		if (portalDoesNotExist(closestPortal)) {
			hidePortalProjection(player);
			return;
		}
		
		Vector portalDistance = closestPortal.getLocation().subtract(playerEyeLoc).toVector();
		
		if (portalDistance.lengthSquared() > main.getPortalDisplayRangeSquared()) {
			hidePortalProjection(player);
			return;
		}
		
		Portal lastViewedPortal = getViewedPortal(player);
		
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
	
	private boolean portalDoesNotExist(Portal portal) {
		
		if (portal == null) {
			return true;
		}
		
		if (!portalHandler.quickCheckExists(portal)) {
			portalHandler.removePortal(portal);
			return true;
		}
		
		return false;
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
		
		viewedPortals.put(player.getUniqueId(), portal);
		viewedPortalSides.put(player.getUniqueId(), projection);
		
		ViewFrustum playerFrustum = ViewFrustumFactory.createFrustum(playerEyeLoc.toVector(), portal.getPortalRect(), projection.getCacheLength());
		
		displayProjectionBlocks(player, portal, projection, playerFrustum, displayFrustum, hidePortalBlocks);
		toggleEntityVisibilities(player, projection, playerFrustum, displayFrustum);
	}
	
	/**
	 * Puts all blocks to display to the player in one map
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
		
		displayBlocks(player, visibleBlocks);
	}
	
	private void toggleEntityVisibilities(Player player,
	                                      ProjectionCache projection,
	                                      ViewFrustum playerFrustum,
	                                      boolean displayFrustum) {
		
		Set<Entity> hiddenEntities = new HashSet<>();
		
		if (playerFrustum == null || !displayFrustum) {
			hideEntities(player, hiddenEntities);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD + "no blocks visible"));
			return;
		}
		
		boolean displayState = true;
		
		for (Entity entity : getNearbyEntities(player)) {
			
			EntityBoundingBox box = getBoundingBox(entity);
			
			if (displayState) {
				boolean one = BoundingBoxUtils.boxIntersectsBlockCache(box, projection);
				boolean two = BoundingBoxUtils.boxIntersectsFrustum(box, playerFrustum);
				
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
						ChatColor.DARK_GRAY + " box: " + ChatColor.GOLD + one +
						ChatColor.DARK_GRAY + " frustum: " + ChatColor.GOLD + two));
			}
			
			if (BoundingBoxUtils.boxIntersectsBlockCache(box, projection) &&
			    BoundingBoxUtils.boxIntersectsFrustum(box, playerFrustum)) {
				hiddenEntities.add(entity);
			}
			
			displayState = false;
		}

//		if (getNearbyEntities(player).size() == 0) {
//			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "" + nearbyEntities.size() + " entities - " + hiddenEntities.size() + " hidden"));
//		}
		hideEntities(player, hiddenEntities);
	}
	
	/**
	 * Forwards the changes made in a block cache to all the linked projection caches. This also live-updates what the players see.
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockType> updatedBlocks) {
		
		Bukkit.broadcastMessage(ChatColor.GRAY + "Updating " + portalHandler.getProjectionsLinkedTo(cache).size() + " projections");
		for (ProjectionCache projection : portalHandler.getProjectionsLinkedTo(cache)) {
			
			Map<BlockVec, BlockType> projectionUpdates = updateProjection(projection, updatedBlocks);
			
			//TODO stop iterating same players for each projection?
			for (UUID playerID : viewedPortalSides.keySet()) {
				
				if (viewedPortalSides.get(playerID) != projection) {
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
				getProjectedBlocks(player).putAll(newBlocksInFrustum);
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
		
		Map<BlockVec, BlockType> viewSession = getProjectedBlocks(player);
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
	
	private void hideEntities(Player player, Set<Entity> newHiddenEntities) {
		
		Set<Entity> hiddenEntities = getHiddenEntities(player);
		Set<Entity> visibleEntities = new HashSet<>();
		
		Iterator<Entity> entityIter = hiddenEntities.iterator();
		
		while (entityIter.hasNext()) {
			
			Entity entity = entityIter.next();
			
			if (!newHiddenEntities.contains(entity)) {
				visibleEntities.add(entity);
				entityIter.remove();
			}
		}
		
		newHiddenEntities.removeIf(hiddenEntities::contains);
		hiddenEntities.addAll(newHiddenEntities);
		
		packetHandler.showEntities(player, visibleEntities);
		packetHandler.hideEntities(player, hiddenEntities);
	}
	
	/**
	 * Removes all to a portal related animations.
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getPortalsLinkedTo(portal);
		affectedPortals.add(portal);
		
		HashMap<UUID, Portal> viewPortalCopy = new HashMap<>(viewedPortals);
		
		for (Map.Entry<UUID, Portal> entry : viewPortalCopy.entrySet()) {
			
			if (affectedPortals.contains(entry.getValue())) {
				hidePortalProjection(Bukkit.getPlayer(entry.getKey()));
			}
		}
	}
}