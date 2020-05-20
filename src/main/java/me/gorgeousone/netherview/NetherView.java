package me.gorgeousone.netherview;

import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.cmdframework.handlers.CommandHandler;
import me.gorgeousone.netherview.commmands.EnableDebugCommand;
import me.gorgeousone.netherview.commmands.ListPortalsCommand;
import me.gorgeousone.netherview.commmands.PortalInfoCommand;
import me.gorgeousone.netherview.commmands.ReloadCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.listeners.BlockListener;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.TeleportListener;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.updatechecks.UpdateCheck;
import me.gorgeousone.netherview.updatechecks.VersionResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class NetherView extends JavaPlugin {
	
	private static final int resourceId = 78885;
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String OPERATE_PERM = "netherview.operate";
	
	private boolean isLegacyServer;
	private Material portalMaterial;
	
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	private Set<UUID> worldsWithProjectingPortals;
	private Set<UUID> viewableOnlyWorlds;
	
	private int portalProjectionDist;
	private int portalDisplayRangeSquared;
	
	private boolean hidePortalBlocks;
	private boolean cancelTeleportWhenLinking;
	
	private boolean debugMessagesEnabled;
	
	@Override
	public void onEnable() {
		
		new Metrics(this, 7571);
		
		loadServerVersion();
		BlockType.configureVersion(isLegacyServer);
		PortalLocator.configureVersion(portalMaterial);
		
		loadConfig();
		loadConfigData();
		
		portalHandler = new PortalHandler(this);
		viewingHandler = new ViewingHandler(this, portalHandler);
		
		registerCommands();
		registerListeners();
		checkForUpdates();
		
		PortalLocator.setDebugModeEnabled(debugMessagesEnabled);
	}
	
	@Override
	public void onDisable() {
		viewingHandler.reset();
	}
	
	public void reload() {
		
		portalHandler.reset();
		viewingHandler.reset();
		
		loadConfig();
		loadConfigData();
		checkForUpdates();
		
		PortalLocator.setDebugModeEnabled(debugMessagesEnabled);
	}
	
	public int getPortalProjectionDist() {
		return portalProjectionDist;
	}
	
	public int getPortalDisplayRangeSquared() {
		return portalDisplayRangeSquared;
	}
	
	public boolean hidePortalBlocks() {
		return hidePortalBlocks;
	}
	
	public boolean cancelTeleportWhenLinking() {
		return cancelTeleportWhenLinking;
	}
	
	public boolean canViewOtherWorlds(World world) {
		return worldsWithProjectingPortals.contains(world.getUID());
	}
	
	public boolean canBeViewed(World world) {
		return viewableOnlyWorlds.contains(world.getUID());
	}
	
	public boolean isDebugMessagesEnabled() {
		return debugMessagesEnabled;
	}
	
	private void registerCommands() {
		
		ParentCommand netherViewCommand = new ParentCommand("netherview", OPERATE_PERM, false, "just tab");
		netherViewCommand.addChild(new ReloadCommand(netherViewCommand, this));
		netherViewCommand.addChild(new EnableDebugCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ListPortalsCommand(netherViewCommand, this, portalHandler));
		netherViewCommand.addChild(new PortalInfoCommand(netherViewCommand, portalHandler));
		
		
		CommandHandler cmdHandler = new CommandHandler(this);
		cmdHandler.registerCommand(netherViewCommand);
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
	
	private void loadConfig() {
		
		reloadConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	private void loadConfigData() {
		
		portalProjectionDist = getConfig().getInt("portal-projection-view-distance", 8);
		portalDisplayRangeSquared = (int) Math.pow(getConfig().getInt("portal-display-range", 32), 2);
		
		hidePortalBlocks = getConfig().getBoolean("hide-portal-blocks", true);
		cancelTeleportWhenLinking = getConfig().getBoolean("cancel-teleport-when-linking-portals", true);
		debugMessagesEnabled = getConfig().getBoolean("debug-messages", false);
		
		worldsWithProjectingPortals = new HashSet<>();
		viewableOnlyWorlds = new HashSet<>();
		
		List<String> worldNames = getConfig().getStringList("worlds-with-projecting-portals");
		
		for (String worldName : worldNames) {
			
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().log(Level.WARNING, "Could not find world " + worldName + ".");
			} else {
				worldsWithProjectingPortals.add(world.getUID());
			}
		}
		
		worldNames = getConfig().getStringList("viewable-worlds");
		
		for (String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().log(Level.WARNING, "Could not find world " + worldName + ".");
			} else {
				viewableOnlyWorlds.add(world.getUID());
			}
		}
	}
	
	public void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(this, portalHandler), this);
		manager.registerEvents(new PlayerMoveListener(this, viewingHandler), this);
		manager.registerEvents(new BlockListener(this, portalHandler, viewingHandler, portalMaterial), this);
	}
	
	private void checkForUpdates() {
		
		new UpdateCheck(this, resourceId).handleResponse((versionResponse, newVersion) -> {
			
			if (versionResponse == VersionResponse.FOUND_NEW) {
				
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.isOp()) {
						player.sendMessage("A new version of NetherView is available: " + ChatColor.LIGHT_PURPLE + newVersion);
					}
				}
				
				getLogger().info("A new version of NetherView is available: " + newVersion);
				
			} else if (versionResponse == VersionResponse.UNAVAILABLE) {
				getLogger().info("Unable to check for new versions...");
			}
		}).check();
	}
	
	public void enableDebugMessages(boolean b) {
		this.debugMessagesEnabled = b;
	}
	
	public Set<UUID> getWorldsWithPortals() {
		return worldsWithProjectingPortals;
	}
}