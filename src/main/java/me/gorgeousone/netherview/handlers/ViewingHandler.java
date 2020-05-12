package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCopy;
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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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
	
	private Map<UUID, Portal> viewedPortals;
	private Map<UUID, ProjectionCache> viewedProjections;
	private Map<UUID, Set<BlockCopy>> playerViewSessions;
	
	public ViewingHandler(Main main, PortalHandler portalHandler) {
		
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
		
		for (BlockCopy copy : getViewSession(player))
			hideBlock(player, copy);
		
		playerViewSessions.remove(player.getUniqueId());
	}
	
	public void displayNearestPortalTo(Player player, Location playerEyeLoc) {
		
		Portal portal = portalHandler.getNearestPortal(playerEyeLoc);
		
		if (portal == null) {
			hideViewSession(player);
			return;
		}
		
		Vector portalDistance = portal.getLocation().subtract(playerEyeLoc).toVector();
		
		if (portalDistance.lengthSquared() > main.getPortalDisplayRangeSquared()) {
			hideViewSession(player);
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
	
//	private Set<BlockCopy> getAllBlocks(ProjectionCache cache) {
//
//		Set<BlockCopy> allBlocks = new HashSet<>();
//
//		BlockVec min = cache.getMin();
//		BlockVec max = cache.getMax();
//
//		for (int x = min.getX(); x <= max.getX(); x++) {
//			for (int y = min.getY(); y <= max.getY(); y++) {
//				for (int z = min.getZ(); z <= max.getZ(); z++) {
//
//					BlockCopy copy = cache.getCopyAt(new BlockVec(x, y, z));
//
//					if (copy != null)
//						allBlocks.add(copy);
//				}
//			}
//		}
//
//		return allBlocks;
//	}
	
	private Set<BlockCopy> getBlocksInFrustum(ProjectionCache projection, ViewingFrustum frustum) {
		
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
		
		for (ProjectionCache projection : portalHandler.getLinkedProjections(cache)) {
			
			Set<BlockCopy> projectionUpdates = new HashSet<>();
			
			for (BlockCopy updatedCopy : updatedCopies)
				projectionUpdates.add(projection.updateCopy(updatedCopy));
			
			World projectionWorld = projection.getWorld();
			
			//TODO iterate through view-session players
			for (Player player : projectionWorld.getPlayers()) {
				
				if (viewedProjections.get(player.getUniqueId()) != projection)
					continue;
				
				Portal portal = viewedPortals.get(player.getUniqueId());
				ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum2(player.getEyeLocation().toVector(), portal.getPortalRect());
				
				for (BlockCopy blockCopy : projectionUpdates) {
					
					if (playerFrustum.contains(blockCopy.getPosition().toVector()))
						player.sendBlockChange(blockCopy.getPosition().toLocation(projectionWorld), blockCopy.getBlockData());
				}
			}
		}
	}
	
	public void refreshProjection(Portal portal, BlockCopy blockCopy) {
		
		World portalWorld = portal.getWorld();
		
		new BukkitRunnable() {
			@Override
			public void run() {
				
				for (Player player : portalWorld.getPlayers()) {
					
					if(!hasViewSession(player) || viewedPortals.get(player.getUniqueId()) != portal)
						continue;
					
					if(getViewSession(player).contains(blockCopy)) {
						displayBlockCopy(player, blockCopy);
					}
				}
			}
		}.runTask(main);
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
				hideBlock(player, nextCopy);
				iterator.remove();
			}
		}
		
		blocksToDisplay.removeIf(blockCopy -> !viewSession.add(blockCopy));
		
		for (BlockCopy blockCopy : blocksToDisplay)
			displayBlockCopy(player, blockCopy);
	}
	
	private void displayBlockCopy(Player player, BlockCopy blockCopy) {
		player.sendBlockChange(blockCopy.getPosition().toLocation(player.getWorld()), blockCopy.getBlockData());
	}
	private void hideBlock(Player player, BlockCopy blockCopy) {
		
		Location blockLoc = blockCopy.getPosition().toLocation(player.getWorld());
		player.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
	}
	
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getLinkedPortals(portal);
		affectedPortals.add(portal);
		Iterator<Map.Entry<UUID, Portal>> iter = viewedPortals.entrySet().iterator();
		
		while (iter.hasNext()) {
			
			Map.Entry<UUID, Portal> playerView = iter.next();
			
			if (!affectedPortals.contains(playerView.getValue()))
				continue;
			
			hideViewSession(Bukkit.getPlayer(playerView.getKey()));
			iter.remove();
		}
	}
}