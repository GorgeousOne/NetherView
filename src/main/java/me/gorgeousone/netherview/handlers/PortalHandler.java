package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.ConsoleUtils;
import me.gorgeousone.netherview.wrapping.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
	
	private NetherView main;
	
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, Long> recentlyViewedPortals;
	
	private BukkitRunnable expirationTimer;
	private long cacheExpirationDuration;
	
	public PortalHandler(NetherView main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		recentlyViewedPortals = new HashMap<>();
		cacheExpirationDuration = Duration.ofMinutes(10).toMillis();
	}
	
	public void reset() {
		
		worldsWithPortals.clear();
		recentlyViewedPortals.clear();
		
		expirationTimer.cancel();
		expirationTimer = null;
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
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
	public Integer getRecentlyViewedPortalsCount() {
		return recentlyViewedPortals.size();
	}
	
	/**
	 * Returns the first portal that contains the passed block as part of the portal surface.
	 * If none was found it will  be tried to add the portal related to this block.
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
	public Portal getNearestPortal(Location playerLoc, boolean mustBeLinked) {
		
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
	 * Returns a Set of all portals connected with their projections to the passed portal. Returns an empty set if none was found.
	 */
	public Set<Portal> getPortalsLinkedTo(Portal portal) {
		
		Set<Portal> linkedToPortals = new HashSet<>();
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			for (Portal secondPortal : worldsWithPortals.get(worldID)) {
				
				if (secondPortal == portal) {
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
		
		boolean isFrontCache = portal.getFrontCache() == cache;
		
		for (Portal linkedPortal : getPortalsLinkedTo(portal)) {
			
			if (linkedPortal.projectionsAreLoaded()) {
				linkedToProjections.add(isFrontCache ? linkedPortal.getBackProjection() : linkedPortal.getFrontProjection());
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
		
		ConsoleUtils.printDebug("Located portal at " + portal.toString());
		return portal;
	}
	
	
	private void loadBlockCachesOf(Portal portal) {
		
		portal.setBlockCaches(BlockCacheFactory.createBlockCaches(
				portal,
				main.getPortalProjectionDist(),
				main.getWorldBorderBlockType(portal.getWorld().getEnvironment())));
		
		addPortalToExpirationTimer(portal);
		
		
		ConsoleUtils.printDebug("Loaded block data for portal " + portal.toString());
	}
	
	
	public void loadProjectionCachesOf(Portal portal) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		Portal counterPortal = portal.getCounterPortal();
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		portal.setTpTransform(linkTransform.clone().invert());
		
		if (!counterPortal.blockCachesAreLoaded()) {
			loadBlockCachesOf(counterPortal);
		}
		
		BlockCache frontCache = counterPortal.getFrontCache();
		BlockCache backCache = counterPortal.getBackCache();
		
		portal.setProjectionCaches(BlockCacheFactory.createProjectionCaches(frontCache, backCache, linkTransform));
		addPortalToExpirationTimer(portal);
	}
	
	private void addPortalToExpirationTimer(Portal portal) {
		
		recentlyViewedPortals.put(portal, System.currentTimeMillis());
		
		if (expirationTimer == null) {
			startCacheExpirationTimer();
		}
	}
	
	public void updateExpirationTime(Portal portal) {
		recentlyViewedPortals.put(portal, System.currentTimeMillis());
	}
	
	/**
	 * Removes all references to a registered portal
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> linkedToPortals = getPortalsLinkedTo(portal);
		
		ConsoleUtils.printDebug("Removing portal at " + portal.toString());
		ConsoleUtils.printDebug("Un-linking " + linkedToPortals.size() + " portal projections.");
		
		for (Portal linkedPortal : linkedToPortals) {
			linkedPortal.removeLink();
		}
		
		recentlyViewedPortals.remove(portal);
		getPortals(portal.getWorld()).remove(portal);
		
		portal.removeLink();
	}
	
	/**
	 * Links a portal to it's counter portal it teleports to.
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			ConsoleUtils.printDebug("Cannot connect portal with size "
			                        + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal with size "
			                        + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are not the same size.");
		}
		
		portal.setLinkedTo(counterPortal);
		
		ConsoleUtils.printDebug("Linked portal "
		                        + portal.toString() + " to portal "
		                        + counterPortal.toString());
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		portalConfig.set("portal-locations", null);
		portalConfig.set("linked-portals", null);
		
		ConfigurationSection portalLocations = portalConfig.createSection("portal-locations");
		ConfigurationSection portalLinks = portalConfig.createSection("linked-portals");
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			
			List<String> portalsInWorld = new ArrayList<>();
			
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				portalsInWorld.add(new BlockVec(portal.getLocation()).toString());
				
				if (portal.isLinked()) {
					portalLinks.set(String.valueOf(portal.hashCode()), portal.getCounterPortal().hashCode());
				}
			}
			
			portalLocations.set(worldID.toString(), portalsInWorld);
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-locations")) {
			return;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		for (String worldID : portalLocations.getKeys(false)) {
			
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				main.getLogger().warning("Could not find world with ID: '" + worldID + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (!main.canCreatePortalViews(worldWithPortals)) {
				continue;
			}
			
			List<String> portalBlocksLocs = portalLocations.getStringList(worldID);
			
			for (String serializedBlockVec : portalBlocksLocs) {
				
				try {
					BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
					addPortalStructure(worldWithPortals.getBlockAt(portalLoc.getX(), portalLoc.getY(), portalLoc.getZ()));
					
				} catch (IllegalArgumentException | IllegalStateException e) {
					main.getLogger().warning("Unable to load portal at [" + worldWithPortals.getName() + ", " + serializedBlockVec + "]: " + e.getMessage());
				}
			}
		}
	}
	
	public void loadPortalLinks(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("linked-portals")) {
			return;
		}
		
		ConfigurationSection portalLinks = portalConfig.getConfigurationSection("linked-portals");
		
		for (String portalHashString : portalLinks.getKeys(false)) {
			
			Portal portal = getPortalByHashCode(Integer.parseInt(portalHashString));
			Portal counterPortal = getPortalByHashCode(portalLinks.getInt(portalHashString));
			
			if (portal != null && counterPortal != null) {
				linkPortalTo(portal, counterPortal);
			}
		}
	}
	
	/**
	 * Calculates a Transform that is needed to translate and rotate block types at the positions of the block cache
	 * of the counter portal to the related position in the projection cache of the portal.
	 */
	private Transform calculateLinkTransform(Portal portal, Portal counterPortal) {
		
		Transform linkTransform = new Transform();
		BlockVec portalLoc1 = portal.getMinBlock();
		BlockVec portalLoc2;
		Axis counterPortalAxis = counterPortal.getAxis();
		
		if (portal.getAxis() == counterPortalAxis) {
			
			portalLoc2 = counterPortal.getMaxBlock();
			portalLoc2.setY(counterPortal.getMinBlock().getY());
			linkTransform.setRotY180Deg();
			
		} else {
			portalLoc2 = counterPortal.getMinBlock();
			
			if (counterPortalAxis == Axis.X) {
				linkTransform.setRotY90DegRight();
			} else {
				linkTransform.setRotY90DegLeft();
			}
		}
		
		linkTransform.setRotCenter(portalLoc2);
		linkTransform.setTranslation(portalLoc1.subtract(portalLoc2));
		return linkTransform;
	}
	
	/**
	 * Starts a scheduler that handles the removal of block caches (and projection caches) that weren't used for a certain expiration time.
	 */
	private void startCacheExpirationTimer() {
		
		ConsoleUtils.printDebug("Starting cache expiration timer");
		
		expirationTimer = new BukkitRunnable() {
			@Override
			public void run() {
				
				Iterator<Map.Entry<Portal, Long>> entries = recentlyViewedPortals.entrySet().iterator();
				long now = System.currentTimeMillis();
				
				while (entries.hasNext()) {
					
					Map.Entry<Portal, Long> entry = entries.next();
					long timeSinceLastUse = now - entry.getValue();
					Portal portal = entry.getKey();
					
					if (timeSinceLastUse > cacheExpirationDuration) {
						
						portal.removeProjectionCaches();
						portal.removeBlockCaches();
						entries.remove();
						ConsoleUtils.printDebug("Removed cached blocks of portal " + portal.toString());
					}
				}
				
				if (recentlyViewedPortals.isEmpty()) {
					this.cancel();
					expirationTimer = null;
				}
			}
		};
		
		expirationTimer.runTaskTimerAsynchronously(main, ticksTillNextMinute(), 10 * 20);
	}
	
	private long ticksTillNextMinute() {
		
		LocalTime now = LocalTime.now();
		LocalTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
		return now.until(nextMinute, ChronoUnit.MILLIS) / 50;
	}
}