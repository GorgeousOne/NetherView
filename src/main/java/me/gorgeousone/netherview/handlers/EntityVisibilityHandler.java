package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that runs a BukkitRunnable to check the entities nearby portals and their movements.
 * It will show/hide entities that walk in/out of areas viewed by players from portals.
 * It also creates movement animations for projected entities for players.
 */
public class EntityVisibilityHandler {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final PacketHandler packetHandler;
	
	private BukkitRunnable entityMotionChecker;
	private final Map<BlockCache, Set<Entity>> entitiesInBlockCaches;
	private final Map<Entity, Location> lastEntityLocs;
	
	public EntityVisibilityHandler(NetherViewPlugin main, ViewHandler viewHandler, PacketHandler packetHandler) {
		
		this.main = main;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		this.entitiesInBlockCaches = new HashMap<>();
		this.lastEntityLocs = new HashMap<>();
		
		if (main.isEntityHidingEnabled()) {
			startEntityCheckerChecker();
		}
	}
	
	public void reload() {
		
		disable();
		
		if (main.isEntityHidingEnabled()) {
			startEntityCheckerChecker();
		}
	}
	
	public void disable() {
		
		entitiesInBlockCaches.clear();
		lastEntityLocs.clear();
		entityMotionChecker.cancel();
	}
	
	private void startEntityCheckerChecker() {
		
		MessageUtils.printDebug("Starting entity visibility timer");
		
		entityMotionChecker = new BukkitRunnable() {
			@Override
			public void run() {
				
				Map<ProjectionCache, Set<PlayerViewSession>> portalSideViewers = viewHandler.getSessionsSortedByPortalSides();
				
				for (ProjectionCache projection : portalSideViewers.keySet()) {
					
					Set<Entity> currentEntities = projection.getEntities();
					
					for (PlayerViewSession session : portalSideViewers.get(projection)) {
						
						showEntitiesNextToFrustum(session);
						hideEntitiesInFrustum(session, currentEntities);
					}
				}
				
				if (!main.isEntityViewingEnabled()) {
					return;
				}
				
				Map<BlockCache, Set<ProjectionCache>> watchedBlockCaches = getProjectionsSortedByBlockCaches(portalSideViewers.keySet());
				
				for (BlockCache blockCache : watchedBlockCaches.keySet()) {
					
					Set<Entity> currentEntities = blockCache.getEntities();
					Set<Entity> lastEntities = entitiesInBlockCaches.get(blockCache);
					
					if (lastEntities == null) {
						entitiesInBlockCaches.put(blockCache, currentEntities);
						continue;
					}
					
					if (lastEntities.isEmpty() && currentEntities.isEmpty()) {
						continue;
					}
					
					Map<Entity, Location> movingEntities = getMovingEntities(currentEntities, lastEntityLocs);
					
					for (ProjectionCache projection : watchedBlockCaches.get(blockCache)) {
						for (PlayerViewSession session : portalSideViewers.get(projection)) {
							
							if (session.getLastViewFrustum() == null) {
								continue;
							}
							
							hideEntitiesOutsideProjection(session, currentEntities);
							projectNewEntitiesInsideProjection(session, currentEntities);
							displayEntityMovements(session, movingEntities);
						}
					}
					
					entitiesInBlockCaches.put(blockCache, currentEntities);
					currentEntities.forEach(entity -> lastEntityLocs.put(entity, entity.getLocation()));
				}
				
				entitiesInBlockCaches.entrySet().removeIf(entry -> !watchedBlockCaches.containsKey(entry.getKey()));
			}
		};
		
		entityMotionChecker.runTaskTimer(main, 0, 3);
	}
	
	private Map<BlockCache, Set<ProjectionCache>> getProjectionsSortedByBlockCaches(Set<ProjectionCache> projections) {
		
		Map<BlockCache, Set<ProjectionCache>> sortedSources = new HashMap<>();
		
		for (ProjectionCache projection : projections) {
			
			BlockCache source = projection.getSourceCache();
			sortedSources.computeIfAbsent(source, set -> new HashSet<>());
			sortedSources.get(source).add(projection);
		}
		
		return sortedSources;
	}
	
	private Map<Entity, Location> getMovingEntities(Collection<Entity> entities, Map<Entity, Location> lastEntityLocs) {
		
		Map<Entity, Location> movingEntities = new HashMap<>();
		
		for (Entity entity : entities) {
			
			if (!lastEntityLocs.containsKey(entity)) {
				continue;
			}
			
			Location newLoc = entity.getLocation();
			Location lastLoc = lastEntityLocs.get(entity).clone();
			//workaround for entities that that moved but tped with a portal to another world
			lastLoc.setWorld(newLoc.getWorld());
			
			if (!newLoc.equals(lastLoc)) {
				movingEntities.put(entity, newLoc.clone().subtract(lastLoc));
			}
		}
		
		return movingEntities;
	}
	
	private void hideEntitiesOutsideProjection(PlayerViewSession session, Set<Entity> newEntities) {
		
		for (Entity entity : new HashSet<>(session.getProjectedEntities().keySet())) {
			
			if (!newEntities.contains(entity) || !session.isVisibleInProjection(entity)) {
				viewHandler.destroyProjectedEntity(session.getPlayer(), entity);
			}
		}
	}
	
	private void projectNewEntitiesInsideProjection(PlayerViewSession session, Set<Entity> newEntities) {
		
		for (Entity entity : newEntities) {
			
			if (!session.getProjectedEntities().containsKey(entity) && session.isVisibleInProjection(entity)) {
				viewHandler.projectEntity(session.getPlayer(), entity, session.getViewedPortalSide().getLinkTransform());
			}
		}
	}
	
	private void displayEntityMovements(PlayerViewSession session, Map<Entity, Location> movedEntities) {
		
		Player player = session.getPlayer();
		ProjectionCache projection = session.getViewedPortalSide();
		Transform linkTransform = projection.getLinkTransform();
		Map<Entity, Location> projectedEntities = session.getProjectedEntities();
		
		for (Entity entity : movedEntities.keySet()) {
			
			boolean entityIsVisibleInProjection = session.isVisibleInProjection(entity);
			
			if (!projectedEntities.containsKey(entity)) {
				
				if (entityIsVisibleInProjection) {
					viewHandler.projectEntity(session.getPlayer(), entity, linkTransform);
				}
				continue;
			}
			
			if (entityIsVisibleInProjection) {
				
				Location newEntityLoc = entity.getLocation();
				Location lastEntityLoc = projectedEntities.get(entity);
				
				Location projectedMovement = linkTransform.rotateLoc(newEntityLoc.clone().subtract(lastEntityLoc));
				projectedEntities.put(entity, newEntityLoc);
				
				packetHandler.sendEntityMoveLook(
						session.getPlayer(),
						entity,
						projectedMovement.toVector(),
						projectedMovement.getYaw(),
						projectedMovement.getPitch(),
						entity.isOnGround());
				
			} else {
				viewHandler.destroyProjectedEntity(player, entity);
			}
		}
	}
	
	private void showEntitiesNextToFrustum(PlayerViewSession session) {
		
		for (Entity entity : new HashSet<>(session.getHiddenEntities())) {
			
			if (!session.isHiddenBehindProjection(entity)) {
				viewHandler.showEntity(session.getPlayer(), entity);
			}
		}
	}
	
	private void hideEntitiesInFrustum(PlayerViewSession session, Set<Entity> newEntities) {
		
		for (Entity entity : newEntities) {
			
			if (!session.getHiddenEntities().contains(entity) && session.isHiddenBehindProjection(entity)) {
				viewHandler.hideEntity(session.getPlayer(), entity);
			}
		}
	}
}