package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.api.PortalLinkEvent;
import me.gorgeousone.netherview.api.PortalUnlinkEvent;
import me.gorgeousone.netherview.api.UnlinkReason;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blockcache.TransformFactory;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler class for storing portals and managing their cached blocks.
 */
public class PortalHandler {
	
	private final NetherViewPlugin main;
	private final Material portalMaterial;
	
	private final Map<UUID, Set<Portal>> worldsWithPortals;
	private final Map<Portal, Long> loadedPortals;
	private BukkitRunnable expirationTimer;
	
	public PortalHandler(NetherViewPlugin main, Material portalMaterial) {
		
		this.main = main;
		this.portalMaterial = portalMaterial;
		
		worldsWithPortals = new HashMap<>();
		loadedPortals = new HashMap<>();
		
		startCacheExpirationTimer();
	}
	
	public void reload() {
		
		disable();
		startCacheExpirationTimer();
	}
	
	public void disable() {
		
		worldsWithPortals.clear();
		loadedPortals.clear();
		expirationTimer.cancel();
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public Set<Portal> getLoadedPortals() {
		return loadedPortals.keySet();
	}
	
	public boolean hasPortals(World world) {
		return worldsWithPortals.containsKey(world.getUID());
	}
	
	/**
	 * Returns the count of currently registered portals of the server
	 */
	public Integer getTotalPortalCount() {
		
		int portalCount = 0;
		
		for (Map.Entry<UUID, Set<Portal>> entry : worldsWithPortals.entrySet()) {
			portalCount += entry.getValue().size();
		}
		
		return portalCount;
	}
	
	/**
	 * Returns the count of portals that have been viewed in the last 10 minutes.
	 */
	public Integer getLoadedPortalsCount() {
		return loadedPortals.size();
	}
	
	/**
	 * Returns the first portal that contains the passed block as part of the portal surface.
	 * If none was found it will be tried to add the portal related to this block.
	 */
	public Portal getPortalByBlock(Block portalBlock) {
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.getPortalBlocks().contains(portalBlock)) {
				return portal;
			}
		}
		
		return addPortalStructure(portalBlock);
	}
	
	/**
	 * Returns the first portal matching the passed hashcode. Returns null if none was found.
	 * (Portal hash codes are based on the location of the portal block with the lowest coordinates)
	 */
	public Portal getPortalByHashCode(int portalHashCode) {
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				if (portal.hashCode() == portalHashCode) {
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
	
	/**
	 * Returns true if a random portal block of the portal still exists. It's meant to be a small test for assuring
	 * that a portal simply still exists for portal viewing.
	 */
	public boolean quickCheckExists(Portal portal) {
		return portal.getPortalBlocks().iterator().next().getType() == portalMaterial;
	}
	
	/**
	 * Returns a Set of all portals connected with their projections to the passed portal. Returns an empty set if none was found.
	 */
	public Set<Portal> getPortalsLinkedTo(Portal portal) {
		
		Set<Portal> linkedToPortals = new HashSet<>();
		
		for (UUID worldId : worldsWithPortals.keySet()) {
			for (Portal secondPortal : worldsWithPortals.get(worldId)) {
				
				if (secondPortal.equals(portal)) {
					continue;
				}
				
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
		
		boolean isBlockCacheFront = portal.getFrontCache().equals(cache);
		
		for (Portal linkedPortal : getPortalsLinkedTo(portal)) {
			
			boolean isLinkedProjectionBack = isBlockCacheFront ^ linkedPortal.isViewFlipped() ^ main.portalsAreFlippedByDefault();
			
			if (linkedPortal.projectionsAreLoaded()) {
				linkedToProjections.add(isLinkedProjectionBack ? linkedPortal.getBackProjection() : linkedPortal.getFrontProjection());
			}
		}
		
		return linkedToProjections;
	}
	
	/**
	 * Locates and registers a new portal.
	 *
	 * @param portalBlock one block of the structure required to detect the rest of it
	 */
	public Portal addPortalStructure(Block portalBlock) {
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		UUID worldID = portal.getWorld().getUID();
		
		worldsWithPortals.putIfAbsent(worldID, new HashSet<>());
		worldsWithPortals.get(worldID).add(portal);
		
		MessageUtils.printDebug("Located portal at " + portal.toString());
		return portal;
	}
	
	
	private void loadBlockCachesOf(Portal portal) {
		
		portal.setBlockCaches(BlockCacheFactory.createBlockCaches(
				portal,
				main.getPortalProjectionDist(),
				main.getWorldBorderBlockType(portal.getWorld().getEnvironment())));
		
		addPortalToExpirationTimer(portal);
		
		MessageUtils.printDebug("Loaded block data for portal " + portal.toString());
	}
	
	public void loadProjectionCachesOf(Portal portal) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		Portal counterPortal = portal.getCounterPortal();
		boolean linkTransformIsFlipped = portal.isViewFlipped() ^ main.portalsAreFlippedByDefault();
		Transform linkTransform = TransformFactory.calculateLinkTransform(portal, counterPortal, linkTransformIsFlipped);
		
		portal.setTpTransform(linkTransform.clone().invert());
		
		if (!counterPortal.blockCachesAreLoaded()) {
			loadBlockCachesOf(counterPortal);
		}
		
		BlockCache frontCache = counterPortal.getFrontCache();
		BlockCache backCache = counterPortal.getBackCache();
		
		if (linkTransformIsFlipped) {
			portal.setProjectionCaches(BlockCacheFactory.createProjectionCaches(backCache, frontCache, linkTransform));
		} else {
			portal.setProjectionCaches(BlockCacheFactory.createProjectionCaches(frontCache, backCache, linkTransform));
		}
		addPortalToExpirationTimer(portal);
	}
	
	private void addPortalToExpirationTimer(Portal portal) {
		loadedPortals.put(portal, System.currentTimeMillis());
	}
	
	public void updateExpirationTime(Portal portal) {
		loadedPortals.put(portal, System.currentTimeMillis());
	}
	
	/**
	 * Removes all references to a registered portal
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> linkedToPortals = getPortalsLinkedTo(portal);
		
		MessageUtils.printDebug("Removing portal at " + portal.toString());
		MessageUtils.printDebug("Un-linking " + linkedToPortals.size() + " portal projections.");
		
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
	
	/**
	 * Links a portal to it's counter portal it teleports to.
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal, Player player) {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			MessageUtils.printDebug("Cannot connect portal with size "
			                        + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal with size "
			                        + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			
			throw new IllegalStateException(ChatColor.GRAY + "These portals are not the same size.");
		}
		
		if (player != null) {
			
			PortalLinkEvent linkEvent = new PortalLinkEvent(portal, counterPortal, player);
			Bukkit.getPluginManager().callEvent(linkEvent);
			
			if (linkEvent.isCancelled()) {
				return;
			}
		}
		
		portal.setLinkedTo(counterPortal);
		
		MessageUtils.printDebug("Linked portal "
		                        + portal.toString() + " to portal "
		                        + counterPortal.toString());
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		portalConfig.set("plugin-version", main.getDescription().getVersion());
		portalConfig.set("portal-locations", null);
		portalConfig.set("portal-data", null);
		
		ConfigurationSection portalLocations = portalConfig.createSection("portal-locations");
		ConfigurationSection portalData = portalConfig.createSection("portal-data");
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			
			List<String> portalsInWorld = new ArrayList<>();
			
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				int portalHash = portal.hashCode();
				
				portalsInWorld.add(new BlockVec(portal.getLocation()).toString());
				portalData.set(portalHash + ".is-flipped", portal.isViewFlipped());
				
				if (portal.isLinked()) {
					portalData.set(portalHash + ".link", portal.getCounterPortal().hashCode());
				}
			}
			
			portalLocations.set(worldID.toString(), portalsInWorld);
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
		
		boolean portalsHaveBeenLoaded = loadPortalLocations(portalConfig);
		
		if (!portalsHaveBeenLoaded) {
			return;
		}
		
		if (!portalConfig.contains("plugin-version")) {
			loadDeprecatedPortalLinks(portalConfig);
		} else {
			loadPortalData(portalConfig);
		}
	}
	
	private boolean loadPortalLocations(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-locations")) {
			return false;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		boolean somePortalsCouldBeLoaded = false;
		
		for (String worldID : portalLocations.getKeys(false)) {
			
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				main.getLogger().warning("Could not find world with ID: '" + worldID + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (!main.canCreatePortalViews(worldWithPortals)) {
				continue;
			}
			
			boolean somePortalsWereLoaded = deserializePortals(worldWithPortals, portalLocations.getStringList(worldID));
			
			if (somePortalsWereLoaded) {
				somePortalsCouldBeLoaded = true;
			}
		}
		
		return somePortalsCouldBeLoaded;
	}
	
	private boolean deserializePortals(World world, List<String> portalLocs) {
		
		boolean somePortalsCouldBeLoaded = false;
		
		for (String serializedBlockVec : portalLocs) {
			
			try {
				BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
				addPortalStructure(world.getBlockAt(portalLoc.getX(), portalLoc.getY(), portalLoc.getZ()));
				somePortalsCouldBeLoaded = true;
				
			} catch (IllegalArgumentException | IllegalStateException e) {
				main.getLogger().warning("Unable to load portal at [" + world.getName() + ", " + serializedBlockVec + "]: " + e.getMessage());
			}
		}
		
		return somePortalsCouldBeLoaded;
	}
	
	private void loadPortalData(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-data")) {
			return;
		}
		
		ConfigurationSection portalData = portalConfig.getConfigurationSection("portal-data");
		
		for (String portalHashString : portalData.getKeys(false)) {
			
			Portal portal = getPortalByHashCode(Integer.parseInt(portalHashString));
			
			if (portal == null) {
				continue;
			}
			
			portal.setViewFlipped(portalData.getBoolean(portalHashString + ".is-flipped"));
			
			if (portalData.contains(portalHashString + ".link")) {
				
				Portal counterPortal = getPortalByHashCode(portalData.getInt(portalHashString + ".link"));
				
				if (counterPortal == null) {
					continue;
				}
				
				linkPortalTo(portal, counterPortal, null);
			}
		}
	}
	
	private void loadDeprecatedPortalLinks(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("linked-portals")) {
			return;
		}
		
		ConfigurationSection portalLinks = portalConfig.getConfigurationSection("linked-portals");
		
		for (String portalHashString : portalLinks.getKeys(false)) {
			
			Portal portal = getPortalByHashCode(Integer.parseInt(portalHashString));
			Portal counterPortal = getPortalByHashCode(portalLinks.getInt(portalHashString));
			
			if (portal != null && counterPortal != null) {
				linkPortalTo(portal, counterPortal, null);
			}
		}
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
		
		expirationTimer.runTaskTimerAsynchronously(main, ticksTillNextMinute(), timerPeriod);
	}
	
	private long ticksTillNextMinute() {
		
		LocalTime now = LocalTime.now();
		LocalTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
		return now.until(nextMinute, ChronoUnit.MILLIS) / 50;
	}
}