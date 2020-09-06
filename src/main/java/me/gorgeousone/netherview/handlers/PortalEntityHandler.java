package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PortalEntityHandler {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	
	private BukkitRunnable entityMovementChecker;
	private final Map<Portal, Set<Entity>> entitiesNearPortals;
	
	public PortalEntityHandler(PortalHandler portalHandler, NetherViewPlugin main) {
		
		this.main = main;
		this.portalHandler = portalHandler;
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
					
					int entityDist = 2 * portal.getFrontProjection().getCacheLength() + (int) portal.getPortalRect().width();
					Collection<Entity> nearbyEntities = portal.getWorld().getNearbyEntities(portal.getLocation(), entityDist, entityDist, entityDist);
					
					
				}
				
				entitiesNearPortals.keySet().removeIf(portal -> !loadedPortals.contains(portal));
			}
		};
		
		entityMovementChecker.runTaskTimer(main, 0, 3);
	}
}
