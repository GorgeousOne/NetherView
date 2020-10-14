package me.gorgeousone.netherview;

import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ConfigSettings {
	
	private final JavaPlugin plugin;
	
	private boolean allWorldsCanCreatePortalViews = false;
	private Set<UUID> portalViewWhiteList;
	private Set<UUID> portalViewBlackList;
	
	private boolean allWorldsCanCreateCustomPortals = false;
	private Set<UUID> customPortalWhiteList;
	private Set<UUID> customPortalBlackList;
	
	private int portalProjectionDist;
	private int portalDisplayRangeSquared;
	
	private boolean portalsAreFlippedByDefault;
	private boolean hidePortalBlocks;
	private boolean cancelTeleportWhenLinking;
	private boolean instantTeleportEnabled;
	private boolean warningMessagesEnabled;
	private boolean debugMessagesEnabled;
	private boolean entityHidingEnabled;
	private boolean entityViewingEnabled;
	
	private HashMap<World.Environment, BlockType> worldBorderBlockTypes;
	
	public ConfigSettings(JavaPlugin plugin, FileConfiguration config) {
		this.plugin = plugin;
		
		hidePortalBlocks = config.getBoolean("hide-portal-blocks");
		cancelTeleportWhenLinking = config.getBoolean("cancel-teleport-when-linking-portals");
		instantTeleportEnabled = config.getBoolean("instant-teleport");
		entityHidingEnabled = config.getBoolean("hide-entities-behind-portals");
		entityViewingEnabled = config.getBoolean("show-entities-inside-portals");
		portalsAreFlippedByDefault = config.getBoolean("flip-portals-by-default");
	}
	
	private void addVersionSpecificDefaults(FileConfiguration config) {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			config.addDefault("overworld-border", "stained_clay");
			config.addDefault("nether-border", "stained_clay:14");
			config.addDefault("end-border", "wool:15");
		
		} else {
			config.addDefault("overworld-border", "white_terracotta");
			config.addDefault("nether-border", "red_concrete");
			config.addDefault("end-border", "black_concrete");
		}
	}
	
	private void loadGeneralSettings(FileConfiguration config) {
		
		int portalDisplayRange = clamp(config.getInt("portal-display-range"), 2, 128);
		portalDisplayRangeSquared = (int) Math.pow(portalDisplayRange, 2);
		portalProjectionDist = clamp(config.getInt("portal-projection-distance"), 1, 32);
		
//		PortalLocator.setMaxPortalSize(clamp(config.getInt("max-portal-size"), 3, 21));
		
		worldBorderBlockTypes = new HashMap<>();
		worldBorderBlockTypes.put(World.Environment.NORMAL, deserializeWorldBorderBlockType(config, "overworld-border"));
		worldBorderBlockTypes.put(World.Environment.NETHER, deserializeWorldBorderBlockType(config, "nether-border"));
		worldBorderBlockTypes.put(World.Environment.THE_END, deserializeWorldBorderBlockType(config, "end-border"));
	}
	
	private void loadNetherPortalSettings(FileConfiguration config) {
		
		portalViewWhiteList = new HashSet<>();
		portalViewBlackList = new HashSet<>();
		
		allWorldsCanCreatePortalViews = deserializeWorldList(portalViewWhiteList, config, "nether-portal-viewing.white-listed-worlds", "nether portal whitelist");
		deserializeWorldList(portalViewWhiteList, config, "nether-portal-viewing.blacklisted-worlds", "nether portal blacklist");
		
		if (allWorldsCanCreatePortalViews) {
			MessageUtils.printDebug("Nether portal viewing enabled in all worlds except black listed ones.");
		}
	}
	
	private void loadCustomPortalSettings(FileConfiguration config) {
		
		customPortalWhiteList = new HashSet<>();
		customPortalBlackList = new HashSet<>();
		
		allWorldsCanCreateCustomPortals = deserializeWorldList(portalViewWhiteList, config, "custom-portal-viewing.white-listed-worlds", "custom portal whitelist");
		deserializeWorldList(portalViewWhiteList, config, "custom-portal-viewing.whitelisted-worlds", "custom portal blacklist");
		
		if (allWorldsCanCreateCustomPortals) {
			MessageUtils.printDebug("Custom portal viewing enabled in all worlds except black listed ones.");
		}
	}
	
	private BlockType deserializeWorldBorderBlockType(FileConfiguration config, String configPath) {
		
		String configValue = config.getString(configPath);
		String defaultValue = config.getDefaults().getString(configPath);
		BlockType worldBorder;
		
		try {
			worldBorder = BlockType.of(configValue);
			
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "'" + configValue + "' could not be interpreted as a block type. Using '" + defaultValue + "' instead.");
			return BlockType.of(defaultValue);
		}
		
		if (!worldBorder.isOccluding()) {
			plugin.getLogger().log(Level.WARNING, "'" + configValue + "' is not an occluding block. Using '" + defaultValue + "' instead.");
			return BlockType.of(defaultValue);
		}
		
		return worldBorder;
	}
	
	private boolean deserializeWorldList(Set<UUID> setToPopulate, FileConfiguration config, String configPath, String listName) {
		
		List<String> worldNames = config.getStringList(configPath);
		
		if (worldNames.contains("*")) {
			return true;
		}
	
		worldNames.forEach(worldName -> addWorld(worldName, setToPopulate, listName));
		return false;
	}
	
	private void addWorld(String worldName, Collection<UUID> worldCollection, String listName) {
		
		World world = Bukkit.getWorld(worldName);
		
		if (world == null) {
			plugin.getLogger().log(Level.WARNING, "Could not find world '" + worldName + "' from " + listName);
		
		} else {
			worldCollection.add(world.getUID());
			MessageUtils.printDebug("Added '" + worldName + "' to " + listName);
		}
	}
	
	/**
	 * Returns the approximate "radius" for the projections of the portals. The final side size of a projection will change
	 * depending to the size of the portal.
	 */
	public int getPortalProjectionDist() {
		return portalProjectionDist;
	}
	
	/**
	 * Returns the squared radius in which the view of a portal will be displayed to players.
	 */
	public int getPortalDisplayRangeSquared() {
		return portalDisplayRangeSquared;
	}
	
	/**
	 * Returns true if hiding the purple portal blocks when seeing a portal view is enabled in the config.
	 */
	public boolean hidePortalBlocksEnabled() {
		return hidePortalBlocks;
	}
	
	public boolean cancelTeleportWhenLinkingPortalsEnabled() {
		return cancelTeleportWhenLinking;
	}
	
	public boolean isInstantTeleportEnabled() {
		return instantTeleportEnabled;
	}
	
	public boolean isEntityHidingEnabled() {
		return entityHidingEnabled;
	}
	
	public boolean isEntityViewingEnabled() {
		return entityViewingEnabled;
	}
	
	public boolean portalsAreFlippedByDefault() {
		return portalsAreFlippedByDefault;
	}
	
	public boolean canCreatePortalViews(World world) {
		
		UUID worldId = world.getUID();
		return !portalViewBlackList.contains(worldId) && (allWorldsCanCreateCustomPortals || portalViewWhiteList.contains(worldId));
	}
	
	public boolean canCreateCustomPortals(World world) {
		
		UUID worldId = world.getUID();
		return !customPortalBlackList.contains(worldId) && (allWorldsCanCreateCustomPortals || customPortalWhiteList.contains(worldId));
	}
	
	public BlockType getWorldBorderBlockType(World.Environment environment) {
		return worldBorderBlockTypes.get(environment);
	}
	
	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}
}
