package me.gorgeousone.netherview;

import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ConfigSettings {
	
	private final JavaPlugin plugin;
	
	private int portalProjectionDist;
	private int portalDisplayRangeSquared;
	private int maxPortalSize;
	private HashMap<World.Environment, BlockType> worldBorderBlockTypes;
	
	private boolean entityHidingEnabled;
	private boolean entityViewingEnabled;
	private int entityUpdateTicks;
	
	private boolean warningMessagesEnabled;
	private boolean debugMessagesEnabled;
	
	private boolean allWorldsCanCreateCustomPortals = false;
	private Set<UUID> customPortalWhiteList;
	private Set<UUID> customPortalBlackList;
	
	private boolean allWorldsCanCreatePortalViews = false;
	private Set<UUID> portalViewWhiteList;
	private Set<UUID> portalViewBlackList;
	
	private boolean netherPortalsAreFlippedByDefault;
	private boolean hidePortalBlocks;
	private boolean cancelTeleportWhenLinking;
	private boolean instantTeleportEnabled;
	
	public ConfigSettings(JavaPlugin plugin, FileConfiguration config) {
		this.plugin = plugin;
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
	
	public int getMaxPortalSize() {
		return maxPortalSize;
	}
	
	public boolean isEntityHidingEnabled() {
		return entityHidingEnabled;
	}
	
	public boolean isEntityViewingEnabled() {
		return entityViewingEnabled;
	}
	
	public int getEntityUpdateTicks() {
		return entityUpdateTicks;
	}
	
	public BlockType getWorldBorderBlockType(World.Environment environment) {
		return worldBorderBlockTypes.get(environment);
	}
	
	public boolean canCreateCustomPortals(World world) {
		
		UUID worldId = world.getUID();
		return !customPortalBlackList.contains(worldId) && (allWorldsCanCreateCustomPortals || customPortalWhiteList.contains(worldId));
	}
	
	public boolean canCreatePortalViews(World world) {
		
		UUID worldId = world.getUID();
		return !portalViewBlackList.contains(worldId) && (allWorldsCanCreatePortalViews || portalViewWhiteList.contains(worldId));
	}
	
	public boolean portalsAreFlippedByDefault() {
		return netherPortalsAreFlippedByDefault;
	}
	
	/**
	 * Returns true if hiding the purple portal blocks when seeing a portal view is enabled in the config.
	 */
	public boolean hidePortalBlocksEnabled() {
		return hidePortalBlocks;
	}
	
	public boolean isInstantTeleportEnabled() {
		return instantTeleportEnabled;
	}
	
	public boolean cancelTeleportWhenLinkingPortalsEnabled() {
		return cancelTeleportWhenLinking;
	}
	
	public boolean areWarningMessagesEnabled() {
		return warningMessagesEnabled;
	}
	
	public boolean areDebugMessagesEnabled() {
		return debugMessagesEnabled;
	}
	
	public void setWarningMessagesEnabled(boolean warningMessagesEnabled) {
		this.warningMessagesEnabled = warningMessagesEnabled;
	}
	
	public void setDebugMessagesEnabled(boolean debugMessagesEnabled) {
		this.debugMessagesEnabled = debugMessagesEnabled;
	}
	
	public void addVersionSpecificDefaults(FileConfiguration config) {
		
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
	
	public void loadGeneralSettings(FileConfiguration config) {
		
		int portalDisplayRange = clamp(config.getInt("portal-display-range"), 2, 128);
		portalDisplayRangeSquared = (int) Math.pow(portalDisplayRange, 2);
		portalProjectionDist = clamp(config.getInt("portal-projection-distance"), 1, 32);
		maxPortalSize = clamp(config.getInt("max-portal-size"), 3, 21);
		
		entityHidingEnabled = config.getBoolean("hide-entities-behind-portals");
		entityViewingEnabled = config.getBoolean("show-entities-inside-portals");
		entityUpdateTicks = clamp(config.getInt("entity-update-ticks"), 1, 3);
		
		warningMessagesEnabled = config.getBoolean("warning-messages");
		debugMessagesEnabled = config.getBoolean("debug-messages");
		
		worldBorderBlockTypes = new HashMap<>();
		worldBorderBlockTypes.put(World.Environment.NORMAL, deserializeWorldBorderBlockType(config, "overworld-border"));
		worldBorderBlockTypes.put(World.Environment.NETHER, deserializeWorldBorderBlockType(config, "nether-border"));
		worldBorderBlockTypes.put(World.Environment.THE_END, deserializeWorldBorderBlockType(config, "end-border"));
	}
	
	public void loadNetherPortalSettings(FileConfiguration config) {
		
		ConfigurationSection configSection = config.getConfigurationSection("nether-portal-viewing");
		
		netherPortalsAreFlippedByDefault = configSection.getBoolean("flip-portals-by-default");
		hidePortalBlocks = configSection.getBoolean("hide-portal-blocks");
		cancelTeleportWhenLinking = configSection.getBoolean("cancel-teleport-when-linking-portals");
		instantTeleportEnabled = configSection.getBoolean("instant-teleport");
		
		portalViewWhiteList = new HashSet<>();
		portalViewBlackList = new HashSet<>();
		
		allWorldsCanCreatePortalViews = deserializeWorldList(portalViewWhiteList, configSection, "whitelisted-worlds", "nether portal whitelist");
		deserializeWorldList(portalViewBlackList, configSection, "blacklisted-worlds", "nether portal blacklist");
		
		if (allWorldsCanCreatePortalViews) {
			MessageUtils.printDebug("Nether portal viewing enabled in all worlds except black listed ones.");
		}
	}
	
	public void loadCustomPortalSettings(FileConfiguration config) {
		
		ConfigurationSection configSection = config.getConfigurationSection("custom-portal-viewing");
		
		customPortalWhiteList = new HashSet<>();
		customPortalBlackList = new HashSet<>();
		
		allWorldsCanCreateCustomPortals = deserializeWorldList(customPortalWhiteList, configSection, "whitelisted-worlds", "custom portal whitelist");
		deserializeWorldList(customPortalBlackList, config, "blacklisted-worlds", "custom portal blacklist");
		
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
	
	private boolean deserializeWorldList(Set<UUID> setToPopulate,
	                                     ConfigurationSection config,
	                                     String configPath,
	                                     String listName) {
		
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
	
	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}
}
