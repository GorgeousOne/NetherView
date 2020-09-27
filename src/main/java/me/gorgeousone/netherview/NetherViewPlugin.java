package me.gorgeousone.netherview;

import com.comphenix.protocol.ProtocolLib;
import me.gorgeousone.netherview.bstats.Metrics;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.cmdframework.handlers.CommandHandler;
import me.gorgeousone.netherview.commmands.FlipPortalCommand;
import me.gorgeousone.netherview.commmands.ListPortalsCommand;
import me.gorgeousone.netherview.commmands.PortalInfoCommand;
import me.gorgeousone.netherview.commmands.ReloadCommand;
import me.gorgeousone.netherview.commmands.ToggleDebugCommand;
import me.gorgeousone.netherview.commmands.TogglePortalViewCommand;
import me.gorgeousone.netherview.commmands.ToggleWarningsCommand;
import me.gorgeousone.netherview.handlers.EntityVisibilityHandler;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.listeners.BlockChangeListener;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.PlayerQuitListener;
import me.gorgeousone.netherview.listeners.TeleportListener;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.updatechecks.UpdateCheck;
import me.gorgeousone.netherview.updatechecks.VersionResponse;
import me.gorgeousone.netherview.utils.ConfigUtils;
import me.gorgeousone.netherview.utils.MessageUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class NetherViewPlugin extends JavaPlugin {
	
	private static final int resourceId = 78885;
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String CONFIG_PERM = "netherview.config";
	public final static String INFO_PERM = "netherview.info";
	public final static String PORTAL_FLIP_PERM = "netherview.flipportal";
	
	public final static String CHAT_PREFIX =
			ChatColor.DARK_RED + "[" +
			ChatColor.DARK_PURPLE + "NV" +
			ChatColor.DARK_RED + "]" +
			ChatColor.LIGHT_PURPLE;
	
	private Material portalMaterial;
	
	private PacketHandler packetHandler;
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	private EntityVisibilityHandler motionHandler;
	
	boolean canAllWorldsCreatePortalViews = false;
	private Set<UUID> worldsWithPortalViewing;
	
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
	
	private Plugin protocolLib = null;
	
	@Override
	public void onEnable() {
		
		if (!loadProtocolLib()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		Metrics metrics = new Metrics(this, 7571);
		registerTotalPortalsChart(metrics);
		registerPortalsOnline(metrics);
		
		portalMaterial = VersionUtils.IS_LEGACY_SERVER ? Material.matchMaterial("PORTAL") : Material.NETHER_PORTAL;
		BlockType.configureVersion(VersionUtils.IS_LEGACY_SERVER);
		PortalLocator.configureVersion(portalMaterial);
		
		loadConfigData();
		loadLangConfigData();
		
		packetHandler = new PacketHandler();
		portalHandler = new PortalHandler(this, portalMaterial);
		viewHandler = new ViewHandler(this, portalHandler, packetHandler);
		motionHandler = new EntityVisibilityHandler(this, viewHandler, packetHandler);
		
		//do not register listeners or commands before creating handlers because the handler references are passed there
		registerListeners();
		registerCommands();
		
		loadSavedPortals();
		checkForUpdates();
	}
	
	private boolean loadProtocolLib() {
		
		protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
		
		if (protocolLib == null || !(protocolLib instanceof ProtocolLib)) {
			
			getLogger().severe("====================================================");
			getLogger().severe("Error: You must have ProtocolLib installed to use");
			getLogger().severe("NetherView! Please download ProtocolLib and then");
			getLogger().severe("restart your server:");
			getLogger().severe("https://www.spigotmc.org/resources/protocollib.1997/");
			getLogger().severe("====================================================");
			return false;
		}
		
		String libVersion = protocolLib.getDescription().getVersion().split("-")[0];
		
		if (VersionUtils.serverIsAtOrAbove("1.16.2") && VersionUtils.versionIsLowerThan(libVersion, "4.6.0")) {
			
			getLogger().severe("============================================================");
			getLogger().severe("Error: For Minecraft 1.16.2 and up Nether View requires at");
			getLogger().severe("least ProtocolLib 4.6.0. This version might be still be a");
			getLogger().severe("development build which can be downloaded here:");
			getLogger().severe("https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/");
			getLogger().severe("============================================================");
			protocolLib = null;
			return false;
		}
		
		return true;
	}
	
	public void reload() {
		
		backupPortals();
		loadConfigData();
		loadLangConfigData();
		
		viewHandler.reload();
		portalHandler.reload();
		motionHandler.reload();
		
		loadSavedPortals();
		checkForUpdates();
	}
	
	@Override
	public void onDisable() {
		
		if (protocolLib == null) {
			return;
		}
		
		backupPortals();
		viewHandler.reload();
		portalHandler.disable();
		motionHandler.disable();
	}
	
	public PortalHandler getPortalHandler() {
		return portalHandler;
	}
	
	public ViewHandler getViewHandler() {
		return viewHandler;
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
		return canAllWorldsCreatePortalViews || worldsWithPortalViewing.contains(world.getUID());
	}
	
	public BlockType getWorldBorderBlockType(World.Environment environment) {
		return worldBorderBlockTypes.get(environment);
	}
	
	public boolean setWarningMessagesEnabled(boolean state) {
		
		if (warningMessagesEnabled != state) {
			
			warningMessagesEnabled = state;
			MessageUtils.setWarningMessagesEnabled(warningMessagesEnabled);
			getConfig().set("warning-messages", warningMessagesEnabled);
			saveConfig();
			return true;
		}
		
		return false;
	}
	
	public boolean setDebugMessagesEnabled(boolean state) {
		
		if (debugMessagesEnabled != state) {
			
			debugMessagesEnabled = state;
			MessageUtils.setDebugMessagesEnabled(debugMessagesEnabled);
			getConfig().set("debug-messages", debugMessagesEnabled);
			saveConfig();
			return true;
		}
		
		return false;
	}
	
	private void registerCommands() {
		
		ParentCommand netherViewCommand = new ParentCommand("netherview", null, false, "just tab");
		netherViewCommand.addChild(new ReloadCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ListPortalsCommand(netherViewCommand, this, portalHandler));
		netherViewCommand.addChild(new PortalInfoCommand(netherViewCommand, this, portalHandler));
		netherViewCommand.addChild(new ToggleDebugCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ToggleWarningsCommand(netherViewCommand, this));
		netherViewCommand.addChild(new TogglePortalViewCommand(viewHandler));
		netherViewCommand.addChild(new FlipPortalCommand(netherViewCommand, this, portalHandler, viewHandler));
		
		CommandHandler cmdHandler = new CommandHandler(this);
		cmdHandler.registerCommand(netherViewCommand);
		cmdHandler.registerCommand(new TogglePortalViewCommand(viewHandler));
	}
	
	private void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(this, portalHandler, viewHandler), this);
		manager.registerEvents(new PlayerMoveListener(this, viewHandler, portalMaterial), this);
		manager.registerEvents(new BlockChangeListener(this, portalHandler, viewHandler, packetHandler, portalMaterial), this);
		manager.registerEvents(new PlayerQuitListener(viewHandler), this);
	}
	
	private void loadConfigData() {
		
		reloadConfig();
		getConfig().options().copyDefaults(true);
		addVersionSpecificDefaults();
		saveConfig();
		
		int portalDisplayRange = clamp(getConfig().getInt("portal-display-range"), 1, 128);
		portalDisplayRangeSquared = (int) Math.pow(portalDisplayRange, 2);
		portalProjectionDist = clamp(getConfig().getInt("portal-view-distance"), 1, 32);
		PortalLocator.setMaxPortalSize(clamp(getConfig().getInt("max-portal-size"), 3, 21));
		
		hidePortalBlocks = getConfig().getBoolean("hide-portal-blocks");
		cancelTeleportWhenLinking = getConfig().getBoolean("cancel-teleport-when-linking-portals");
		instantTeleportEnabled = getConfig().getBoolean("instant-teleport");
		entityHidingEnabled = getConfig().getBoolean("hide-entities-behind-portals");
		entityViewingEnabled = getConfig().getBoolean("show-entities-inside-portals");
		portalsAreFlippedByDefault = getConfig().getBoolean("flip-portals-by-default");
		
		setWarningMessagesEnabled(getConfig().getBoolean("warning-messages"));
		setDebugMessagesEnabled(getConfig().getBoolean("debug-messages"));
		
		loadWorldBorderBlockTypes();
		loadWorldsWithPortalViewing();
	}
	
	private void loadLangConfigData() {
		Message.loadLangConfigValues(ConfigUtils.loadConfig("language", this));
	}
	
	private void addVersionSpecificDefaults() {
		
		if (VersionUtils.serverIsAtOrAbove("1.13.0")) {
			
			getConfig().addDefault("overworld-border", "white_terracotta");
			getConfig().addDefault("nether-border", "red_concrete");
			getConfig().addDefault("end-border", "black_concrete");
			
		} else {
			
			getConfig().addDefault("overworld-border", "stained_clay");
			getConfig().addDefault("nether-border", "stained_clay:14");
			getConfig().addDefault("end-border", "wool:15");
		}
	}
	
	private void loadWorldsWithPortalViewing() {
		
		worldsWithPortalViewing = new HashSet<>();
		List<String> worldNames = getConfig().getStringList("worlds-with-portal-viewing");
		
		if (worldNames.size() == 1 && worldNames.get(0).equals("*")) {
			
			canAllWorldsCreatePortalViews = true;
			MessageUtils.printDebug("Nether View enabled in all worlds");
			return;
		}
		
		for (String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().log(Level.WARNING, "World " + worldName + " could not be found.");
			} else {
				worldsWithPortalViewing.add(world.getUID());
			}
		}
	}
	
	private void loadWorldBorderBlockTypes() {
		
		worldBorderBlockTypes = new HashMap<>();
		worldBorderBlockTypes.put(World.Environment.NORMAL, deserializeWorldBorderBlockType("overworld-border"));
		worldBorderBlockTypes.put(World.Environment.NETHER, deserializeWorldBorderBlockType("nether-border"));
		worldBorderBlockTypes.put(World.Environment.THE_END, deserializeWorldBorderBlockType("end-border"));
	}
	
	private BlockType deserializeWorldBorderBlockType(String configPath) {
		
		String configValue = getConfig().getString(configPath);
		String defaultValue = getConfig().getDefaults().getString(configPath);
		BlockType worldBorder;
		
		try {
			worldBorder = BlockType.of(configValue);
			
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "'" + configValue + "' could not be interpreted as a block type. Using '" + defaultValue + "' instead.");
			return BlockType.of(defaultValue);
		}
		
		if (!worldBorder.isOccluding()) {
			getLogger().log(Level.WARNING, "'" + configValue + "' is not an occluding block. Using '" + defaultValue + "' instead.");
			return BlockType.of(defaultValue);
		}
		
		return worldBorder;
	}
	
	private void loadSavedPortals() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
		
		if (!portalConfigFile.exists()) {
			return;
		}
		
		YamlConfiguration portalConfig = YamlConfiguration.loadConfiguration(portalConfigFile);
		
		try {
			portalHandler.loadPortals(portalConfig);
		} catch (Exception ignored) {}
		
		backupPortals();
	}
	
	public void backupPortals() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
		portalConfigFile.delete();
		
		YamlConfiguration portalConfig = YamlConfiguration.loadConfiguration(portalConfigFile);
		portalHandler.savePortals(portalConfig);
		
		try {
			portalConfig.save(portalConfigFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void registerTotalPortalsChart(Metrics metrics) {
		metrics.addCustomChart(new Metrics.SingleLineChart("total_portals", () -> portalHandler.getTotalPortalCount()));
	}
	
	private void registerPortalsOnline(Metrics metrics) {
		metrics.addCustomChart(new Metrics.SingleLineChart("portals_online", () -> portalHandler.getLoadedPortals().size()));
	}
	
	private void checkForUpdates() {
		
		new UpdateCheck(this, resourceId).handleResponse((versionResponse, newVersion) -> {
			
			if (versionResponse == VersionResponse.FOUND_NEW) {
				
				for (Player player : Bukkit.getOnlinePlayers()) {
					
					if (player.isOp()) {
						player.sendMessage("A new version of NetherView is available: " + ChatColor.LIGHT_PURPLE + newVersion);
					}
				}
				
				Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "A new version of NetherView is available: " + newVersion);
				
			} else if (versionResponse == VersionResponse.UNAVAILABLE) {
				
				getLogger().info("Unable to check for new versions of NetherView...");
			}
		}).check();
	}
	
	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}
}