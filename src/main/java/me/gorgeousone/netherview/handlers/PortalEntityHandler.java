package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapping.WrappedBoundingBox;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalEntityHandler {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	
	private BukkitRunnable entityMovementChecker;
	private final Map<Portal, Collection<Entity>> entitiesNearPortals;
	private final Map<Entity, Location> lastEntityLocs;
	
	
	public PortalEntityHandler(NetherViewPlugin main,
	                           PortalHandler portalHandler,
	                           ViewHandler viewHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
		this.entitiesNearPortals = new HashMap<>();
		this.lastEntityLocs = new HashMap<>();
		
		startMovementChecker();
	}
	
	public void reset() {
	
		disable();
		startMovementChecker();
	}
	
	public void disable() {
	
	
	}
	
	private void startMovementChecker() {
		
		entityMovementChecker = new BukkitRunnable() {
			@Override
			public void run() {
				
				if (isCancelled()) {
					return;
				}
				
				Map<Portal, Collection<Entity>> newEntitiesNearPortals= new HashMap<>();
				Map<Entity, Location> newEntityLocs = new HashMap<>();
				
				Map<Portal, Set<Entity>> entitiesThatMoved = new HashMap<>();
				Map<Portal, Set<Entity>> addedEntities = new HashMap<>();
				Map<Portal, Set<Entity>> removedEntities = new HashMap<>();
				
				for (Portal portal : portalHandler.getLoadedPortals()) {
					
					int entityDist = 2 * portal.getFrontProjection().getCacheLength() + (int) portal.getPortalRect().width();
					Collection<Entity> detectedEntities = portal.getWorld().getNearbyEntities(portal.getLocation(), entityDist, entityDist, entityDist);
					newEntitiesNearPortals.put(portal, detectedEntities);
					
					Collection<Entity> lastEntitiesNearPortal = entitiesNearPortals.get(portal);
					Set<Entity> portalAddedEntities = getAddedEntities(lastEntitiesNearPortal, detectedEntities);
					Set<Entity> portalRemovedEntities = getRemovedEntities(lastEntitiesNearPortal, detectedEntities);
					Map<Entity, Location> portalMovingEntities = getMovingEntities(detectedEntities, portalAddedEntities);
					
					
				}
				
				Map<Entity, WrappedBoundingBox> entityBoxes = new HashMap<>();
				Map<Portal, Set<Player>> portalViewers = viewHandler.getViewersSorted();
				Map<UUID, ViewFrustum> lastFrustums = viewHandler.getLastViewFrustums();
				
			}
		};
		
		entityMovementChecker.runTaskTimer(main, 0, 3);
	}
	
	private Set<Entity> getAddedEntities(Collection<Entity> lastEntities, Collection<Entity> newEntities) {

		Set<Entity> addedEntities = new HashSet<>();

		newEntities.forEach(entity -> {
			if (!lastEntities.contains(entity)) {
				addedEntities.add(entity);
			}
		});

		return addedEntities;
	}
	
	private Set<Entity> getRemovedEntities(Collection<Entity> lastEntities, Collection<Entity> newEntities) {
		
		Set<Entity> removedEntities = new HashSet<>();
		
		lastEntities.forEach(entity -> {
			if (!newEntities.contains(entity)) {
				removedEntities.add(entity);
			}
		});
		
		return removedEntities;
	}
	
	private Map<Entity, Location> getMovingEntities(Collection<Entity> newEntities, Set<Entity> excludedEntities) {
		
		Map<Entity, Location> movingEntities = new HashMap<>();
		
		for (Entity entity : newEntities) {
			
			if (excludedEntities.contains(entity)) {
				continue;
			}
			
			Location newLoc = entity.getLocation();
			Location lastLoc = lastEntityLocs.get(entity);
			
			if (!newLoc.equals(lastLoc)) {
				movingEntities.put(entity, getRelMovement(newLoc, lastLoc));
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
}
