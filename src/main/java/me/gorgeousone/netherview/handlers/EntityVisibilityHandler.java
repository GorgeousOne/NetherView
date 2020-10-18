package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.packet.PacketHandler;
import me.gorgeousone.netherview.portal.ProjectionEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that runs a BukkitRunnable to check on entities nearby portals and their movements.
 * It will show/hide entities that walk in/out of areas viewed by players from portals.
 * It also creates movement animations for projected entities for players.
 */
public class EntityVisibilityHandler {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	
	private final ViewHandler viewHandler;
	private final PacketHandler packetHandler;
	
	private BukkitRunnable entityMotionChecker;
	private final Map<Entity, ProjectionEntity> projectionEntities;
	
	public EntityVisibilityHandler(JavaPlugin plugin,
	                               ConfigSettings configSettings,
	                               ViewHandler viewHandler,
	                               PacketHandler packetHandler) {
		
		this.plugin = plugin;
		this.configSettings = configSettings;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		
		projectionEntities = new HashMap<>();
		
		if (configSettings.isEntityHidingEnabled()) {
			startEntityCheckerChecker();
		}
	}
	
	public void reload() {
		
		disable();
		
		if (configSettings.isEntityHidingEnabled()) {
			startEntityCheckerChecker();
		}
	}
	
	public void disable() {
		
		entityMotionChecker.cancel();
		projectionEntities.clear();
	}
	
	private void startEntityCheckerChecker() {
		
		MessageUtils.printDebug("Starting entity visibility timer");
		
		entityMotionChecker = new BukkitRunnable() {
			@Override
			public void run() {
				
				Map<ProjectionCache, Set<PlayerViewSession>> portalSideViewers = viewHandler.getSessionsSortedByPortalSides();
				handleRealEntitiesVisibility(portalSideViewers);
				
				if (!configSettings.isEntityViewingEnabled()) {
					return;
				}
				
				Map<BlockCache, Set<ProjectionCache>> watchedBlockCaches = getProjectionsSortedByBlockCaches(portalSideViewers.keySet());
				handleProjectionEntitiesVisibility(portalSideViewers, watchedBlockCaches);
				
				projectionEntities.entrySet().removeIf(entry -> entry.getKey().isDead());
				projectionEntities.values().forEach(ProjectionEntity::updateLastLoc);
			}
		};
		
		entityMotionChecker.runTaskTimer(plugin, 0, configSettings.getEntityUpdateTicks());
	}
	
	private void handleRealEntitiesVisibility(Map<ProjectionCache, Set<PlayerViewSession>> portalSideViewers) {
		
		for (ProjectionCache projection : portalSideViewers.keySet()) {
			
			Set<Entity> currentEntities = projection.getEntities();
			
			for (PlayerViewSession session : portalSideViewers.get(projection)) {
				
				showEntitiesNextToFrustum(session);
				hideEntitiesInFrustum(session, currentEntities);
			}
		}
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
	
	private void handleProjectionEntitiesVisibility(Map<ProjectionCache, Set<PlayerViewSession>> portalSideViewers,
	                                                Map<BlockCache, Set<ProjectionCache>> watchedBlockCaches) {
		
		for (BlockCache blockCache : watchedBlockCaches.keySet()) {
			
			Set<Entity> currentEntities = blockCache.getEntities();
			Set<ProjectionEntity> newEntities = getProjectionEntities(currentEntities);
			
			currentEntities.forEach(entity -> projectionEntities.computeIfAbsent(entity, ProjectionEntity::new));
			
			Map<ProjectionEntity, Location> movingEntities = getMovingEntities(currentEntities);
			
			for (ProjectionCache projection : watchedBlockCaches.get(blockCache)) {
				for (PlayerViewSession session : portalSideViewers.get(projection)) {
					
					hideEntitiesOutsideProjection(session, newEntities);
					
					if (session.getLastViewFrustum() == null) {
						continue;
					}
					
					projectNewEntitiesInsideProjection(session, newEntities);
					displayEntityMovements(session, movingEntities);
				}
			}
		}
	}
	
	private Set<ProjectionEntity> getProjectionEntities(Set<Entity> entities) {
		
		Set<ProjectionEntity> projectionEntities = new HashSet<>();
		
		entities.forEach(entity -> {
			this.projectionEntities.computeIfAbsent(entity, ProjectionEntity::new);
			projectionEntities.add(this.projectionEntities.get(entity));
		});
		
		return projectionEntities;
	}
	
	private void hideEntitiesOutsideProjection(PlayerViewSession session, Set<ProjectionEntity> newEntities) {
		
		for (ProjectionEntity entityProjection : new HashSet<>(session.getProjectedEntities())) {
			
			if (!newEntities.contains(entityProjection) || !session.isVisibleInProjection(entityProjection.getEntity())) {
				viewHandler.destroyProjectedEntity(session.getPlayer(), entityProjection);
			}
		}
	}
	
	private void projectNewEntitiesInsideProjection(PlayerViewSession session, Set<ProjectionEntity> newEntities) {
		
		for (ProjectionEntity entityProjection : newEntities) {
			
			if (!session.getProjectedEntities().contains(entityProjection) && session.isVisibleInProjection(entityProjection.getEntity())) {
				viewHandler.projectEntity(session.getPlayer(), entityProjection, session.getViewedPortalSide().getLinkTransform());
			}
		}
	}
	
	private Map<ProjectionEntity, Location> getMovingEntities(Collection<Entity> entities) {
		
		Map<ProjectionEntity, Location> movingEntities = new HashMap<>();
		
		for (Entity entity : entities) {
			
			ProjectionEntity projectionEntity = projectionEntities.get(entity);
			Location lastLoc = projectionEntity.getLastLoc();
			
			if (lastLoc == null) {
				continue;
			}
			
			Location newLoc = entity.getLocation();
			
			if (newLoc.getWorld() == lastLoc.getWorld() && !newLoc.equals(lastLoc)) {
				movingEntities.put(projectionEntity, newLoc.clone().subtract(lastLoc));
			}
		}
		
		return movingEntities;
	}
	
	private void displayEntityMovements(PlayerViewSession session, Map<ProjectionEntity, Location> movedEntities) {
		
		Player player = session.getPlayer();
		ProjectionCache projection = session.getViewedPortalSide();
		Transform linkTransform = projection.getLinkTransform();
		Set<ProjectionEntity> projectedEntities = session.getProjectedEntities();
		
		for (ProjectionEntity entity : movedEntities.keySet()) {
			
			boolean entityIsVisibleInProjection = session.isVisibleInProjection(entity.getEntity());
			
			if (!projectedEntities.contains(entity)) {
				
				if (entityIsVisibleInProjection) {
					viewHandler.projectEntity(session.getPlayer(), entity, linkTransform);
				}
				continue;
			}
			
			if (entityIsVisibleInProjection) {
				
				Location newEntityLoc = entity.getEntity().getLocation();
				Location lastEntityLoc = entity.getLastLoc();
				
				Location projectedMovement = linkTransform.rotateLoc(newEntityLoc.clone().subtract(lastEntityLoc));
				
				packetHandler.sendEntityMoveLook(
						session.getPlayer(),
						entity,
						projectedMovement.toVector(),
						projectedMovement.getYaw(),
						projectedMovement.getPitch(),
						entity.getEntity().isOnGround());
				
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