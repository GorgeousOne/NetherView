package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalHandler {
	
	private NetherView main;
	
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, Set<Portal>> portalsLinkedToPortals;
	private Map<BlockCache, Set<ProjectionCache>> linkedProjections;
	
	private BukkitRunnable expirationTimer;
	private long cacheExpirationDuration;
	private Map<Portal, Long> recentlyViewedPortals;
	
	public PortalHandler(NetherView main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		portalsLinkedToPortals = new HashMap<>();
		linkedProjections = new HashMap<>();
		
		recentlyViewedPortals = new HashMap<>();
		cacheExpirationDuration = Duration.ofMinutes(10).toMillis();
	}
	
	public void reset() {
		
		worldsWithPortals.clear();
		portalsLinkedToPortals.clear();
		linkedProjections.clear();
		recentlyViewedPortals.clear();
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
		return new HashSet<>(portalsLinkedToPortals.getOrDefault(portal, new HashSet<>()));
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
		return linkedProjections.getOrDefault(cache, new HashSet<>());
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
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Located portal at " + portal.toString());
		}
		
		return portal;
	}
	
	private void loadBlockCachesOf(Portal portal) {
		
		portal.setBlockCaches(BlockCacheFactory.createBlockCaches(
				portal,
				main.getPortalProjectionDist(),
				main.getWorldBorderBlockType(portal.getWorld().getEnvironment())));
		
		addPortalToExpirationTimer(portal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Loaded block data for portal " + portal.toString());
		}
	}
	
	public void loadProjectionCachesOf(Portal portal) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		Portal counterPortal = portal.getCounterPortal();
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		if (!counterPortal.blockCachesAreLoaded()) {
			loadBlockCachesOf(counterPortal);
		}
		
		BlockCache cache1 = counterPortal.getFrontCache();
		BlockCache cache2 = counterPortal.getBackCache();
		
		//the projections caches are switching positions because of the transform
		ProjectionCache projection1 = new ProjectionCache(portal, cache2, linkTransform);
		ProjectionCache projection2 = new ProjectionCache(portal, cache1, linkTransform);
		portal.setProjectionCaches(new AbstractMap.SimpleEntry<>(projection1, projection2));
		
		linkedProjections.putIfAbsent(cache1, new HashSet<>());
		linkedProjections.putIfAbsent(cache2, new HashSet<>());
		linkedProjections.get(cache1).add(projection2);
		linkedProjections.get(cache2).add(projection1);
		
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
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Removing portal at " + portal.toString());
		}
		
		//unlink other portals from this portal
		if (portalsLinkedToPortals.containsKey(portal)) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Un-linking " + portalsLinkedToPortals.get(portal).size() + " portal projections.");
			}
			
			//don't use unlink() because that will create a CurrentModificationException
			for (Portal linkedPortal : portalsLinkedToPortals.get(portal)) {
				unregisterProjectionCachesOf(linkedPortal);
				portal.removeLink();
			}
			
			portalsLinkedToPortals.remove(portal);
		}
		
		unlinkPortal(portal);
		unregisterBlockCachesOf(portal);
		
		recentlyViewedPortals.remove(portal);
		getPortals(portal.getWorld()).remove(portal);
	}
	
	/**
	 * Removes the linked portal from the passed portal and unregisters it's projection caches
	 */
	public void unlinkPortal(Portal portal) {
		
		unregisterProjectionCachesOf(portal);
		portalsLinkedToPortals.get(portal.getCounterPortal()).remove(portal);
		portal.removeLink();
	}
	
	/**
	 * Removes the block caches of the passed portal and any projection caches referring to them.
	 */
	private void unregisterBlockCachesOf(Portal portal) {
		
		if (portal.blockCachesAreLoaded()) {
			
			linkedProjections.remove(portal.getFrontCache());
			linkedProjections.remove(portal.getBackCache());
			portal.removeBlockCaches();
			
			if (portalsLinkedToPortals.containsKey(portal)) {
				for (Portal linkedPortal : portalsLinkedToPortals.get(portal)) {
					unregisterProjectionCachesOf(linkedPortal);
				}
			}
		}
	}
	
	/**
	 * Removes the projection caches of the passed portal and takes care of removing places where they are referenced
	 */
	private void unregisterProjectionCachesOf(Portal portal) {
		
		if (portal.isLinked() && portal.projectionsAreLoaded()) {
			
			Portal counterPortal = portal.getCounterPortal();
			
			if(counterPortal.blockCachesAreLoaded()) {
				linkedProjections.get(counterPortal.getFrontCache()).remove(portal.getBackProjection());
				linkedProjections.get(counterPortal.getBackCache()).remove(portal.getFrontProjection());
			}
			
			portal.removeProjectionCaches();
		}
	}
	
	/**
	 * Links a portal to it's counter portal it teleports to.
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Cannot connect portal with size "
				                                      + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal with size "
				                                      + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			}
			
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are not the same size.");
		}
		
		portal.setLinkedTo(counterPortal);
		portalsLinkedToPortals.putIfAbsent(counterPortal, new HashSet<>());
		portalsLinkedToPortals.get(counterPortal).add(portal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Linked portal "
			                                      + portal.toString() + " to portal "
			                                      + counterPortal.toString());
		}
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
					main.getLogger().warning("Unable to load portal at " + worldWithPortals.getName() + ", " + serializedBlockVec + ": " + e.getMessage());
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
		
		Transform linkTransform;
		Vector distance = portal.getLocation().toVector().subtract(counterPortal.getLocation().toVector());
		
		linkTransform = new Transform();
		linkTransform.setTranslation(new BlockVec(distance));
		linkTransform.setRotCenter(new BlockVec(counterPortal.getPortalRect().getMin()));
		
		//during the rotation some weird shifts happen
		//I did not figure out where they come from, for now some extra translations are a good workaround
		if (portal.getAxis() == counterPortal.getAxis()) {
			
			linkTransform.setRotY180Deg();
			int portalBlockWidth = (int) portal.getPortalRect().width() - 1;
			
			if (counterPortal.getAxis() == Axis.X) {
				linkTransform.translate(new BlockVec(portalBlockWidth, 0, 0));
			} else {
				linkTransform.translate(new BlockVec(0, 0, portalBlockWidth));
			}
			
		} else if (counterPortal.getAxis() == Axis.X) {
			linkTransform.setRotY90DegRight();
			linkTransform.translate(new BlockVec(0, 0, 1));
			
		} else {
			linkTransform.setRotY90DegLeft();
			linkTransform.translate(new BlockVec(1, 0, 0));
		}
		
		return linkTransform;
	}
	
	/**
	 * Starts a scheduler that handles the removal of block caches (and projection caches) that weren't used for a certain expiration time.
	 */
	private void startCacheExpirationTimer() {
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Starting cache expiration timer");
		}
		
		expirationTimer = new BukkitRunnable() {
			@Override
			public void run() {
				
				Iterator<Map.Entry<Portal, Long>> entries = recentlyViewedPortals.entrySet().iterator();
				long now = System.currentTimeMillis();
				
				while (entries.hasNext()) {
					
					Map.Entry<Portal, Long> entry = entries.next();
					long timeSinceLastUse = now - entry.getValue();
					Portal portal = entry.getKey();
					
					if (timeSinceLastUse < cacheExpirationDuration) {
						continue;
					}
					
					//remove projection caches if they weren't used for 10 minutes
					unregisterProjectionCachesOf(portal);
					
					if (portal.blockCachesAreLoaded() && getProjectionsLinkedTo(portal.getFrontCache()).isEmpty()) {
						unregisterBlockCachesOf(portal);
					}
					
					if (!portal.projectionsAreLoaded() && !portal.blockCachesAreLoaded()) {
						entries.remove();
						
						if (main.debugMessagesEnabled()) {
							Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Removed cached blocks of portal " + portal.toString());
						}
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