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
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.customportal.CustomPortalSerializer;
import me.gorgeousone.netherview.customportal.PlayerClickListener;
import me.gorgeousone.netherview.customportal.PlayerSelectionHandler;
import me.gorgeousone.netherview.customportal.commands.CreatePortalCommand;
import me.gorgeousone.netherview.customportal.commands.DeletePortalCommand;
import me.gorgeousone.netherview.customportal.commands.LinkPortalCommand;
import me.gorgeousone.netherview.handlers.EntityVisibilityHandler;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.listeners.BlockChangeListener;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.PlayerQuitListener;
import me.gorgeousone.netherview.listeners.PlayerTeleportListener;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.portal.PortalSerializer;
import me.gorgeousone.netherview.updatechecks.UpdateCheck;
import me.gorgeousone.netherview.updatechecks.VersionResponse;
import me.gorgeousone.netherview.utils.ConfigUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class NetherViewPlugin extends JavaPlugin {
	
	private static final int resourceId = 78885;
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String CONFIG_PERM = "netherview.config";
	public final static String INFO_PERM = "netherview.info";
	public final static String PORTAL_FLIP_PERM = "netherview.flipportal";
	public final static String CUSTOM_PORTAL_PERM = "netherview.customportals";
	
	public final static String CHAT_PREFIX =
			ChatColor.DARK_RED + "[" +
			ChatColor.DARK_PURPLE + "NV" +
			ChatColor.DARK_RED + "]" +
			ChatColor.LIGHT_PURPLE;
	
	private ConfigSettings configSettings;
	private Material portalMaterial;
	
	private PacketHandler packetHandler;
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	private EntityVisibilityHandler entityHandler;
	private PlayerSelectionHandler selectionHandler;
	private CustomPortalHandler customPortalHandler;
	
	
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
		
		configSettings = new ConfigSettings(this, getConfig());
		loadConfigSettings();
		loadLangConfigData();
		
		packetHandler = new PacketHandler();
		portalHandler = new PortalHandler(this, configSettings, portalMaterial);
		viewHandler = new ViewHandler(configSettings, portalHandler, packetHandler);
		entityHandler = new EntityVisibilityHandler(this, configSettings, viewHandler, packetHandler);
		selectionHandler = new PlayerSelectionHandler();
		customPortalHandler = new CustomPortalHandler();
		
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
		loadConfigSettings();
		loadLangConfigData();
		
		viewHandler.reload();
		portalHandler.reload();
		entityHandler.reload();
		
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
		entityHandler.disable();
	}
	
	public PortalHandler getPortalHandler() {
		return portalHandler;
	}
	
	public ViewHandler getViewHandler() {
		return viewHandler;
	}
	
	public boolean setWarningMessagesEnabled(boolean state) {
		
		if (configSettings.areWarningMessagesEnabled() != state) {
			
			configSettings.setWarningMessagesEnabled(state);
			MessageUtils.setWarningMessagesEnabled(state);
			getConfig().set("warning-messages", state);
			saveConfig();
			return true;
		}
		
		return false;
	}
	
	public boolean setDebugMessagesEnabled(boolean state) {
		
		if (configSettings.areDebugMessagesEnabled() != state) {
			
			configSettings.setDebugMessagesEnabled(state);
			MessageUtils.setDebugMessagesEnabled(state);
			getConfig().set("debug-messages", state);
			saveConfig();
			return true;
		}
		
		return false;
	}
	
	private void registerCommands() {
		
		ParentCommand netherViewCommand = new ParentCommand("netherview", null, false, "just tab");
		netherViewCommand.addChild(new ReloadCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ListPortalsCommand(netherViewCommand, configSettings, portalHandler));
		netherViewCommand.addChild(new PortalInfoCommand(netherViewCommand, configSettings, portalHandler));
		netherViewCommand.addChild(new ToggleDebugCommand(netherViewCommand, this));
		netherViewCommand.addChild(new ToggleWarningsCommand(netherViewCommand, this));
		netherViewCommand.addChild(new TogglePortalViewCommand(viewHandler));
		netherViewCommand.addChild(new FlipPortalCommand(netherViewCommand, configSettings, portalHandler, viewHandler));
		
		netherViewCommand.addChild(new CreatePortalCommand(netherViewCommand, selectionHandler, portalHandler, customPortalHandler));
		netherViewCommand.addChild(new DeletePortalCommand(netherViewCommand, portalHandler, customPortalHandler));
		netherViewCommand.addChild(new LinkPortalCommand(netherViewCommand, portalHandler, customPortalHandler));
		
		CommandHandler cmdHandler = new CommandHandler(this);
		cmdHandler.registerCommand(netherViewCommand);
		cmdHandler.registerCommand(new TogglePortalViewCommand(viewHandler));
	}
	
	private void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new PlayerTeleportListener(configSettings, portalHandler, viewHandler), this);
		manager.registerEvents(new PlayerMoveListener(this, configSettings, viewHandler, customPortalHandler, portalMaterial), this);
		manager.registerEvents(new BlockChangeListener(this, configSettings, portalHandler, viewHandler, packetHandler, portalMaterial), this);
		manager.registerEvents(new PlayerQuitListener(viewHandler), this);
		
		manager.registerEvents(new PlayerClickListener(selectionHandler, configSettings), this);
	}
	
	private void loadConfigSettings() {
		
		reloadConfig();
		configSettings.addVersionSpecificDefaults(getConfig());
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		configSettings.loadGeneralSettings(getConfig());
		PortalLocator.setMaxPortalSize(configSettings.getMaxPortalSize());
		MessageUtils.setWarningMessagesEnabled(configSettings.areWarningMessagesEnabled());
		MessageUtils.setDebugMessagesEnabled(configSettings.areDebugMessagesEnabled());
		
		configSettings.loadNetherPortalSettings(getConfig());
		configSettings.loadCustomPortalSettings(getConfig());
	}
	
	private void loadLangConfigData() {
		Message.loadLangConfigValues(ConfigUtils.loadConfig("language", this));
	}
	
	private void loadSavedPortals() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
		File customPortalConfigFile = new File(getDataFolder() + File.separator + "custom-portals.yml");
		
		if (!portalConfigFile.exists()) {
			return;
		}
		
		if (!customPortalConfigFile.exists()) {
			return;
		}
		
		YamlConfiguration portalConfig = YamlConfiguration.loadConfiguration(portalConfigFile);
		YamlConfiguration customPortalConfig = YamlConfiguration.loadConfiguration(customPortalConfigFile);
		
		new PortalSerializer(this, configSettings, portalHandler).loadPortals(portalConfig);
		new CustomPortalSerializer(this, configSettings, portalHandler, customPortalHandler).loadPortals(customPortalConfig);
	}
	
	public void backupPortals() {
		
		File portalConfigFile = new File(getDataFolder() + File.separator + "portals.yml");
		File customPortalConfigFile = new File(getDataFolder() + File.separator + "custom-portals.yml");
		
		portalConfigFile.delete();
		customPortalConfigFile.delete();
		
		YamlConfiguration portalConfig = YamlConfiguration.loadConfiguration(portalConfigFile);
		YamlConfiguration customPortalConfig = YamlConfiguration.loadConfiguration(customPortalConfigFile);
		
		new PortalSerializer(this, configSettings, portalHandler).savePortals(portalConfig);
		new CustomPortalSerializer(this, configSettings, portalHandler, customPortalHandler).savePortals(customPortalConfig);
		
		try {
			portalConfig.save(portalConfigFile);
			customPortalConfig.save(customPortalConfigFile);
			
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
}