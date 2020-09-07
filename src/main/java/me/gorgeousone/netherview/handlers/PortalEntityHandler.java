package me.gorgeousone.netherview.handlers;

import javafx.geometry.BoundingBox;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapping.WrappedBoundingBox;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalEntityHandler {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	
	private BukkitRunnable entityMovementChecker;
	private final Map<Portal, Set<Entity>> entitiesNearPortals;
	
	public PortalEntityHandler(NetherViewPlugin main,
	                           PortalHandler portalHandler,
	                           ViewHandler viewHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
		this.entitiesNearPortals = new HashMap<>();
	
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
				
				Set<Portal> loadedPortals = portalHandler.getLoadedPortals();
				
				for (Portal portal : portalHandler.getLoadedPortals()) {
					
					Set<UUID> viewers = viewHandler.getViewers(portal).isEmpty());

					if (viewers.isEmpty()) {
						continue;
					}
					
					int entityDist = 2 * portal.getFrontProjection().getCacheLength() + (int) portal.getPortalRect().width();
					Portal counterPortal = portal.getCounterPortal();
					Collection<Entity> nearbyEntities = counterPortal.getWorld().getNearbyEntities(counterPortal.getLocation(), entityDist, entityDist, entityDist);
					
					Map<WrappedBoundingBox, Entity> entityBounds = new HashMap<>();
					
					for (Entity entity : nearbyEntities) {
						entityBounds.put(WrappedBoundingBox.of(entity), entity);
					}
					
					Transform tpTransform = portal.getTpTransform();
					
					for (UUID playerId : viewers) {
						
						ViewFrustum counterFrustum = tpTransform.getTransformedFrustum(viewHandler.getLastViewFrustum(playerId));
						BlockCache counterCache = viewHandler.getViewedPortalSide(playerId).getSourceCache();
						
						
						for (WrappedBoundingBox boundingBox : entityBounds.keySet()) {
							
							if (boundingBox.intersectsFrustum(counterFrustum) && boundingBox.intersectsBlockCache(counterCache)) {
							
								//hide entity
							}
						}
					}
				}
				
				entitiesNearPortals.keySet().removeIf(portal -> !loadedPortals.contains(portal));
			}
		};
		
		entityMovementChecker.runTaskTimer(main, 0, 3);
	}
}
