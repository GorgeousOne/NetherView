package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.wrapper.WrappedBoundingBox;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityMovementHandler {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final PacketHandler packetHandler;
	
	private BukkitRunnable entityMovementChecker;
	
	private final Map<BlockCache, Set<Entity>> entitiesInSourceCaches;
	private final Map<Entity, Location> lastEntityLocs;
	
	public EntityMovementHandler(NetherViewPlugin main, ViewHandler viewHandler, PacketHandler packetHandler) {
		
		this.main = main;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		this.entitiesInSourceCaches = new HashMap<>();
		this.lastEntityLocs = new HashMap<>();
		
		startMovementChecker();
	}
	
	public void reload() {
		
		disable();
		startMovementChecker();
	}
	
	public void disable() {
		
		entitiesInSourceCaches.clear();
		lastEntityLocs.clear();
		entityMovementChecker.cancel();
	}
	
	private void startMovementChecker() {
		
		entityMovementChecker = new BukkitRunnable() {
			@Override
			public void run() {
				
				if (isCancelled()) {
					return;
				}
				
				Map<ProjectionCache, Set<PlayerViewSession>> projectionSessionsMap = viewHandler.getSessionsSortedByProjectionCaches();
				Map<BlockCache, Set<ProjectionCache>> sourceCacheMap = getProjectionsSortedBySources(projectionSessionsMap.keySet());
				
				for (BlockCache sourceCache : sourceCacheMap.keySet()) {
					
					Set<Entity> newEntities = sourceCache.getEntities();
					Set<Entity> oldEntities = entitiesInSourceCaches.get(sourceCache);
					
					if (oldEntities == null) {
						entitiesInSourceCaches.put(sourceCache, newEntities);
						return;
					}
					
					Map<Entity, Location> movedEntities = getMovedEntities(newEntities, lastEntityLocs);
					
					for (Entity entity : movedEntities.keySet()) {
						
						for (ProjectionCache projection : sourceCacheMap.get(sourceCache)) {
							for (PlayerViewSession session : projectionSessionsMap.get(projection)) {
								
								Player player = session.getPlayer();
								ViewFrustum playerFrustum = session.getLastViewFrustum();
								
								if (entity.equals(player) || playerFrustum == null) {
									continue;
								}
								
								Transform linkTransform = projection.getLinkTransform();
								Location projectionLoc = linkTransform.transformLoc(entity.getLocation());
								
								WrappedBoundingBox boundingBox = WrappedBoundingBox.of(entity, projectionLoc);
								boolean entityIntersectsFrustum = boundingBox.intersectsFrustum(playerFrustum);
								
								Location entityMovement = movedEntities.get(entity);
								
								
								if (session.getProjectedEntities().contains(entity)) {
									
									if (!entityIntersectsFrustum) {
										viewHandler.destroyProjectedEntity(player, entity);
										player.sendMessage(ChatColor.GOLD + "hide " + entity.getType().name().toLowerCase());
										
									} else {
										packetHandler.sendEntityMoveLook(
												player,
												entity,
												entityMovement.toVector(),
												entityMovement.getYaw(),
												entityMovement.getPitch(),
												entity.isOnGround());
									}
									
								} else {
									
									if (entityIntersectsFrustum) {
										viewHandler.projectEntity(session.getPlayer(), entity, projectionLoc, linkTransform);
										player.sendMessage(ChatColor.GOLD + "show " + entity.getType().name().toLowerCase());
									}
								}
							}
						}
					}
					
					entitiesInSourceCaches.put(sourceCache, newEntities);
					newEntities.forEach(entity -> lastEntityLocs.put(entity, entity.getLocation()));
				}
				
				entitiesInSourceCaches.entrySet().removeIf(entry -> !sourceCacheMap.containsKey(entry.getKey()));
			}
		};
		
		entityMovementChecker.runTaskTimer(main, 0, 3);
	}
	
	private Map<BlockCache, Set<ProjectionCache>> getProjectionsSortedBySources(Set<ProjectionCache> projections) {
		
		Map<BlockCache, Set<ProjectionCache>> sortedSources = new HashMap<>();
		
		for (ProjectionCache projection : projections) {
			
			BlockCache source = projection.getSourceCache();
			sortedSources.putIfAbsent(source, new HashSet<>());
			sortedSources.get(source).add(projection);
		}
		
		return sortedSources;
	}
	
	private Map<Entity, Location> getMovedEntities(Collection<Entity> entities, Map<Entity, Location> lastEntityLocs) {
		
		Map<Entity, Location> movingEntities = new HashMap<>();
		
		for (Entity entity : entities) {
			
			if (!lastEntityLocs.containsKey(entity)) {
				continue;
			}
			
			Location newLoc = entity.getLocation();
			Location lastLoc = lastEntityLocs.get(entity);
			
			if (!newLoc.equals(lastLoc)) {
				movingEntities.put(entity, newLoc.clone().subtract(lastLoc));
			}
		}
		
		return movingEntities;
	}
	
	private Location getRelMovement(Location newLoc, Location oldLoc) {
		
		Location relMovement = newLoc.clone().subtract(oldLoc);
		relMovement.setYaw(newLoc.getYaw() - oldLoc.getYaw());
		relMovement.setPitch(newLoc.getPitch() - oldLoc.getPitch());
		return relMovement;
	}
	
	private Set<Entity> getEntitiesInFrustum(ViewFrustum frustum, Collection<Entity> nearbyEntities) {
		
		Set<Entity> visibleEntities = new HashSet<>();
		
		return null;
	}

//	private Set<Entity> getAddedEntities(Collection<Entity> lastEntities, Collection<Entity> newEntities) {
//
//		Set<Entity> addedEntities = new HashSet<>();
//
//		newEntities.forEach(entity -> {
//			if (!lastEntities.contains(entity)) {
//				addedEntities.add(entity);
//			}
//		});
//
//		return addedEntities;
//	}
//
//	private Set<Entity> getRemovedEntities(Collection<Entity> lastEntities, Collection<Entity> newEntities) {
//
//		Set<Entity> removedEntities = new HashSet<>();
//
//		lastEntities.forEach(entity -> {
//			if (!newEntities.contains(entity)) {
//				removedEntities.add(entity);
//			}
//		});
//
//		return removedEntities;
//	}
}
