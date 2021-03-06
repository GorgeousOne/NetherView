package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blockcache.TransformFactory;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.event.PortalLinkEvent;
import me.gorgeousone.netherview.event.PortalUnlinkEvent;
import me.gorgeousone.netherview.event.UnlinkReason;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler class for storing portals and managing their cached blocks.
 */
public class PortalHandler {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	private final Material portalMaterial;
	
	private final Map<UUID, Set<Portal>> portalInWorlds;
	
	private final Map<Portal, Long> loadedPortals;
	private BukkitRunnable expirationTimer;
	
	public PortalHandler(JavaPlugin main,
	                     ConfigSettings configSettings,
	                     Material portalMaterial) {
		
		this.plugin = main;
		this.configSettings = configSettings;
		this.portalMaterial = portalMaterial;
		
		portalInWorlds = new HashMap<>();
		loadedPortals = new HashMap<>();
		
		startCacheExpirationTimer();
	}
	
	public void reload() {
		
		disable();
		startCacheExpirationTimer();
	}
	
	public void disable() {
		
		portalInWorlds.clear();
		loadedPortals.clear();
		expirationTimer.cancel();
	}
	
	public Set<Portal> getPortals(World world) {
		return portalInWorlds.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public Set<Portal> getLoadedPortals() {
		return loadedPortals.keySet();
	}
	
	public boolean hasPortals(World world) {
		return portalInWorlds.containsKey(world.getUID());
	}
	
	/**
	 * Returns the count of currently registered portals of the server
	 */
	public Integer getTotalPortalCount() {
		
		int portalCount = 0;
		
		for (Map.Entry<UUID, Set<Portal>> entry : portalInWorlds.entrySet()) {
			portalCount += entry.getValue().size();
		}
		
		return portalCount;
	}
	
	/**
	 * Returns the first portal that contains the passed block as part of the portal surface.
	 * If none was found it will be tried to add the portal related to this block.
	 */
	public Portal getPortalAt(Block portalBlock) throws MessageException {
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.getPortalBlocks().contains(portalBlock)) {
				return portal;
			}
		}
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		addPortal(portal);
		return portal;
	}
	
	/**
	 * Returns the first portal matching the passed hashcode. Returns null if none was found.
	 * (Portal hash codes are based on the location of the portal block with the lowest coordinates)
	 */
	public Portal getPortalByHash(int portalHash) {
		
		for (UUID worldID : portalInWorlds.keySet()) {
			for (Portal portal : portalInWorlds.get(worldID)) {
				
				if (portal.hashCode() == portalHash) {
					return portal;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the the nearest portal in a world to the passed Location. Returns null if none was found.
	 *
	 * @param mustBeLinked specify if the returned portal should be linked already
	 */
	public Portal getClosestPortal(Location playerLoc, boolean mustBeLinked) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		if (!hasPortals(playerLoc.getWorld())) {
			return null;
		}
		
		for (Portal portal : getPortals(playerLoc.getWorld())) {
			
			if (mustBeLinked && !portal.isLinked()) {
				continue;
			}
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public void addPortal(Portal portal) {
		
		World world = portal.getWorld();
		UUID worldID = portal.getWorld().getUID();
		
		if (portal instanceof CustomPortal) {
			
			if (!configSettings.canCreateCustomPortals(world)) {
				throw new IllegalArgumentException("Custom portals are not enabled in world '" + world.getName() + "'. Cannot add custom portal.");
			}
			
		} else if (!configSettings.canCreatePortalViews(world)) {
			throw new IllegalArgumentException("Portal viewing is not enabled in world '" + world.getName() + "'. Cannot add portal.");
		}
		
		portalInWorlds.computeIfAbsent(worldID, set -> new HashSet<>());
		portalInWorlds.get(worldID).add(portal);
		MessageUtils.printDebug("Added" + (portal instanceof CustomPortal ? " custom " : " ") + "portal at " + portal.toString());
	}
	
	/**
	 * Removes all references to a registered portal
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> linkedToPortals = getPortalsLinkedTo(portal);
		MessageUtils.printDebug("Removing portal at " + portal.toString());
		MessageUtils.printDebug("Un-linking " + linkedToPortals.size() + " portal projections");
		
		for (Portal linkedPortal : linkedToPortals) {
			
			Bukkit.getPluginManager().callEvent(new PortalUnlinkEvent(linkedPortal, portal, UnlinkReason.LINKED_PORTAL_DESTROYED));
			linkedPortal.removeLink();
		}
		
		loadedPortals.remove(portal);
		
		if (portal.isLinked()) {
			
			Bukkit.getPluginManager().callEvent(new PortalUnlinkEvent(portal, portal.getCounterPortal(), UnlinkReason.PORTAL_DESTROYED));
			portal.removeLink();
		}
		
		getPortals(portal.getWorld()).remove(portal);
	}
	
	public boolean portalDoesNotExist(Portal portal) {
		
		if (portal == null) {
			return true;
		}
		
		if (portal instanceof CustomPortal) {
			return false;
		}
		
		if (portal.getPortalBlocks().iterator().next().getType() != portalMaterial) {
			removePortal(portal);
			return true;
		}
		
		return false;
	}
	
	public boolean portalIntersectsOtherPortals(Portal portal) {
		
		for (Portal otherPortal : getPortals(portal.getWorld())) {
			if (otherPortal.getInner().intersects(portal.getInner())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns a Set of all portals connected with their projections to the passed portal. Returns an empty set if none was found.
	 */
	public Set<Portal> getPortalsLinkedTo(Portal portal) {
		
		Set<Portal> linkedToPortals = new HashSet<>();
		
		for (UUID worldId : portalInWorlds.keySet()) {
			for (Portal secondPortal : portalInWorlds.get(worldId)) {
				
				if (secondPortal.isLinked() && secondPortal.getCounterPortal() == portal) {
					linkedToPortals.add(secondPortal);
				}
			}
		}
		
		return linkedToPortals;
	}
	
	/**
	 * Returns all block caches (2 for each portal) of all portals in specified world.
	 */
	public Set<BlockCache> getBlockCaches(World world) {
		
		Set<BlockCache> caches = new HashSet<>();
		
		for (Portal portal : getPortals(world)) {
			
			if (portal.blockCachesAreLoaded()) {
				caches.add(portal.getFrontCache());
				caches.add(portal.getBackCache());
			}
		}
		
		return caches;
	}
	
	/**
	 * Returns a Set of projection caches that are not connected to a portal but to a specific block cache (one of two for a portal).
	 * Returns an empty Set if none were found.
	 */
	public Set<ProjectionCache> getProjectionsLinkedTo(BlockCache cache) {
		
		Set<ProjectionCache> linkedToProjections = new HashSet<>();
		Portal portal = cache.getPortal();
		
		for (Portal linkedPortal : getPortalsLinkedTo(portal)) {
		
			if (linkedPortal.projectionsAreLoaded()) {
		
				ProjectionCache frontProjection = linkedPortal.getFrontProjection();
				linkedToProjections.add(frontProjection.getSourceCache() == cache ? frontProjection : linkedPortal.getBackProjection());
			}
		}
		
		return linkedToProjections;
	}
	
	private void loadBlockCachesOf(Portal portal) {
		
		portal.setBlockCaches(BlockCacheFactory.createBlockCaches(
				portal,
				configSettings.getPortalProjectionDist(),
				configSettings.getWorldBorderBlockType(portal.getWorld().getEnvironment())));
		
		addPortalToExpirationTimer(portal);
		
		MessageUtils.printDebug("Loaded block data for portal " + portal.toString());
	}
	
	public void loadProjectionCachesOf(Portal portal) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		Portal counterPortal = portal.getCounterPortal();
		boolean isLinkTransformFlipped = isLinkTransformFlipped(portal);
		
		Transform linkTransform = TransformFactory.calculateBlockLinkTransform(portal, counterPortal, isLinkTransformFlipped);
		portal.setTpTransform(linkTransform.clone().invert());
		
		if (!counterPortal.blockCachesAreLoaded()) {
			loadBlockCachesOf(counterPortal);
		}
		
		BlockCache frontCache = counterPortal.getFrontCache();
		BlockCache backCache = counterPortal.getBackCache();
		
		if (isLinkTransformFlipped) {
			portal.setProjectionCaches(BlockCacheFactory.createProjectionCaches(portal, backCache, frontCache, linkTransform));
		} else {
			portal.setProjectionCaches(BlockCacheFactory.createProjectionCaches(portal, frontCache, backCache, linkTransform));
		}
		
		addPortalToExpirationTimer(portal);
	}
	
	public boolean isLinkTransformFlipped(Portal portal) {
		return portal.isViewFlipped() ^ (configSettings.portalsAreFlippedByDefault() && !(portal instanceof CustomPortal));
	}
	
	private void addPortalToExpirationTimer(Portal portal) {
		loadedPortals.put(portal, System.currentTimeMillis());
	}
	
	public void updateExpirationTime(Portal portal) {
		loadedPortals.put(portal, System.currentTimeMillis());
	}
	
	/**
	 * Links a portal to it's counter portal it teleports to.
	 *
	 * @param triggerPlayer - player who triggered the portal linking. set to null if no player involved
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal, Player triggerPlayer) throws MessageException {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			MessageUtils.printDebug("Cannot connect portal with size "
			                        + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal with size "
			                        + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			
			throw new MessageException(Message.UNEQUAL_PORTALS);
		}
		
		if (triggerPlayer != null) {
			
			PortalLinkEvent linkEvent = new PortalLinkEvent(portal, counterPortal, triggerPlayer);
			Bukkit.getPluginManager().callEvent(linkEvent);
			
			if (linkEvent.isCancelled()) {
				return;
			}
		}
		
		portal.removeLink();
		portal.setLinkedTo(counterPortal);
		
		MessageUtils.printDebug("Linked" + (portal instanceof CustomPortal ? " custom " : " ") + "portal "
		                        + portal.toString() + " to portal "
		                        + counterPortal.toString());
	}
	
	/**
	 * Starts a scheduler that handles the removal of block caches (and projection caches) that weren't used for a certain expiration time.
	 */
	private void startCacheExpirationTimer() {
		
		long cacheExpirationDuration = Duration.ofMinutes(10).toMillis();
		long timerPeriod = 10 * 20;
		
		MessageUtils.printDebug("Starting cache expiration timer");
		
		expirationTimer = new BukkitRunnable() {
			@Override
			public void run() {
				
				Iterator<Map.Entry<Portal, Long>> entries = loadedPortals.entrySet().iterator();
				long now = System.currentTimeMillis();
				
				while (entries.hasNext()) {
					
					Map.Entry<Portal, Long> entry = entries.next();
					long timeSinceLastUse = now - entry.getValue();
					Portal portal = entry.getKey();
					
					if (timeSinceLastUse > cacheExpirationDuration) {
						
						portal.removeProjectionCaches();
						portal.removeBlockCaches();
						entries.remove();
						MessageUtils.printDebug("Removed cached block data of portal " + portal.toString());
					}
				}
			}
		};
		
		expirationTimer.runTaskTimerAsynchronously(plugin, TimeUtils.getTicksTillNextMinute(), timerPeriod);
	}
}