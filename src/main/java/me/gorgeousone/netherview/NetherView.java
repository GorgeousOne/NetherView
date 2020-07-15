package me.gorgeousone.netherview;

import com.comphenix.protocol.ProtocolLib;
import me.gorgeousone.netherview.bstats.Metrics;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.cmdframework.handlers.CommandHandler;
import me.gorgeousone.netherview.commmands.DebugDisplayPortal;
import me.gorgeousone.netherview.commmands.EnableDebugCommand;
import me.gorgeousone.netherview.commmands.ListPortalsCommand;
import me.gorgeousone.netherview.commmands.PortalInfoCommand;
import me.gorgeousone.netherview.commmands.ReloadCommand;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.listeners.BlockListener;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.PlayerQuitListener;
import me.gorgeousone.netherview.listeners.TeleportListener;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.updatechecks.UpdateCheck;
import me.gorgeousone.netherview.updatechecks.VersionResponse;
import me.gorgeousone.netherview.utils.ConsoleUtils;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
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

public final class NetherView extends JavaPlugin {
	
	private static final int resourceId = 78885;
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String RELOAD_PERM = "netherview.reload";
	public final static String INFO_PERM = "netherview.info";
	
	private boolean isLegacyServer;
	private Material portalMaterial;
	
	private PortalHandler portalHandler;
	private PacketHandler packetHandler;
	private ViewHandler viewHandler;
	
	private Set<UUID> worldsWithPortalViewing;
	
	private int portalProjectionDist;
	private int portalDisplayRangeSquared;
	
	private boolean hidePortalBlocks;
	private boolean cancelTeleportWhenLinking;
	private boolean instantTeleportEnabled;
	private boolean debugMessagesEnabled;
	
	private HashMap<World.Environment, BlockType> worldBorderBlockTypes;
	
	Plugin protocolLib = null;
	
	@Override
	public void onEnable() {
		
		protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
		
		if (protocolLib == null || !(protocolLib instanceof ProtocolLib)) {
			getLogger().severe("====================================================");
			getLogger().severe("Error: You must have ProtocolLib installed to use");
			getLogger().severe("NetherView! Please download ProtocolLib and then");
			getLogger().severe("restart your server:");
			getLogger().severe("https://www.spigotmc.org/resources/protocollib.1997/");
			getLogger().severe("====================================================");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		Metrics metrics = new Metrics(this, 7571);
		registerTotalPortalsChart(metrics);
		registerPortalsOnline(metrics);
		
		loadServerVersion();
		BlockType.configureVersion(isLegacyServer);
		PortalLocator.configureVersion(portalMaterial);
		
		portalHandler = new PortalHandler(this);
		packetHandler = new PacketHandler();
		viewHandler = new ViewHandler(this, portalHandler, packetHandler);
		
		//do not register listeners or commands before creating handlers because the handler references are passed there
		registerListeners();
		registerCommands();
		
		loadConfigData();
		checkForUpdates();
	}
	
	public void reload() {
		
		onDisable();
		loadConfigData();
		checkForUpdates();
	}
	
	@Override
	public void onDisable() {
		
		if (protocolLib == null) {
			return;
		}
		
		savePortalsToConfig();
		viewHandler.reset();
		portalHandler.reset();
	}
	
	public int getPortalProjectionDist() {
		return portalProjectionDist;
	}
	
	public int getPortalDisplayRangeSquared() {
		return portalDisplayRangeSquared;
	}
	
	public boolean hidePortalBlocksEnabled() {
		return hidePortalBlocks;
	}
	
	public boolean cancelTeleportWhenLinkingPortalsEnabled() {
		return cancelTeleportWhenLinking;
	}
	
	public boolean isInstantTeleportEnabled() {
		return instantTeleportEnabled;
	}
	
	public boolean canCreatePortalViews(World world) {
		return worldsWithPortalViewing.contains(world.getUID());
	}
	
	public BlockType getWorldBorderBlockType(World.Environment environment) {
		return worldBorderBlockTypes.get(environment);
	}
	
	public boolean debugMessagesEnabled() {
		return debugMessagesEnabled;
	}
	
	public boolean setDebugMessagesEnabled(boolean state) {
		
		if (debugMessagesEnabled != state) {
			
			debugMessagesEnabled = state;
			ConsoleUtils.setDebugMessagesEnabled(debugMessagesEnabled);
			getConfig().set("debug-messages", debugMessagesEnabled);
			saveConfig();
			return true;
		}
		
		return false;
	}
	
	private void loadServerVersion() {
		
		String version = getServer().getBukkitVersion();
		isLegacyServer =
				version.contains("1.8") ||
				version.contains("1.9") ||
				version.contains("1.10") ||
				version.contains("1.11") ||
				version.contains("1.12");
		
		portalMaterial = isLegacyServer ? Material.matchMaterial("PORTAL") : Material.NETHER_PORTAL;
	}
	
	private void registerCommands() {
		
		ParentCommand netherViewCommand = new ParentCommand("netherview", null, false, "just tab");
		netherViewCommand.addChild(new ReloadCommand(netherViewCommand, this));
		netherViewCommand.addChild(new EnableDebugCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ListPortalsCommand(netherViewCommand, this, portalHandler));
		netherViewCommand.addChild(new PortalInfoCommand(netherViewCommand, portalHandler));
		
		CommandHandler cmdHandler = new CommandHandler(this);
		cmdHandler.registerCommand(netherViewCommand);
		cmdHandler.registerCommand(new DebugDisplayPortal(viewHandler));
	}
	
	private void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(this, portalHandler, viewHandler), this);
//		manager.registerEvents(new PlayerMoveListener(this, viewHandler, portalMaterial), this);
		manager.registerEvents(new BlockListener(this, portalHandler, viewHandler, packetHandler, portalMaterial), this);
		manager.registerEvents(new PlayerQuitListener(viewHandler), this);
	}
	
	private void loadConfigData() {
		
		reloadConfig();
		getConfig().options().copyDefaults(true);
		addVersionSpecificDefaults();
		saveConfig();
		
		int portalDisplayRange = clamp(getConfig().getInt("portal-display-range"), 1, 128);
		portalDisplayRangeSquared = (int) Math.pow(portalDisplayRange, 2);
		portalProjectionDist = clamp(getConfig().getInt("portal-projection-view-distance"), 1, 32);
		hidePortalBlocks = getConfig().getBoolean("hide-portal-blocks");
		cancelTeleportWhenLinking = getConfig().getBoolean("cancel-teleport-when-linking-portals");
		instantTeleportEnabled = getConfig().getBoolean("instant-teleport");
		
		setDebugMessagesEnabled(getConfig().getBoolean("debug-messages"));
		
		loadWorldBorderBlockTypes();
		loadWorldsWithPortalViewing();
		loadRegisteredPortals();
	}
	
	private void addVersionSpecificDefaults() {
		
		if (isLegacyServer) {
			getConfig().addDefault("overworld-border", "stained_clay");
			getConfig().addDefault("nether-border", "stained_clay:14");
			getConfig().addDefault("end-border", "wool:15");
			
		} else {
			getConfig().addDefault("overworld-border", "white_terracotta");
			getConfig().addDefault("nether-border", "red_concrete");
			getConfig().addDefault("end-border", "black_concrete");
		}
	}
	
	private void loadWorldsWithPortalViewing() {
		
		worldsWithPortalViewing = new HashSet<>();
		
		List<String> worldNames = getConfig().getStringList("worlds-with-portal-viewing");
		
		for (String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().log(Level.WARNING, "World " + worldName + " could be found.");
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
	
	private void loadRegisteredPortals() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
		
		if (!portalConfigFile.exists()) {
			return;
		}
		
		YamlConfiguration portalConfig = YamlConfiguration.loadConfiguration(portalConfigFile);
		portalHandler.loadPortals(portalConfig);
		portalHandler.loadPortalLinks(portalConfig);
		
		try {
			portalConfig.save(portalConfigFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void savePortalsToConfig() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
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
		metrics.addCustomChart(new Metrics.SingleLineChart("portals_online", () -> portalHandler.getRecentlyViewedPortalsCount()));
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