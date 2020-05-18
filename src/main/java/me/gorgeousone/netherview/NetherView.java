package me.gorgeousone.netherview;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.listeners.BlockListener;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.TeleportListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class NetherView extends JavaPlugin {
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String RELOAD_PERM = "netherview.reload";
	
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	private Set<UUID> worldsWithProjectingPortals;
	private Set<UUID> viewableOnlyWorlds;
	
	private int portalProjectionDist;
	private int portalDisplayRangeSquared;
	
	private boolean hidePortalBlocks;
	private boolean cancelTeleportWhenLinking;
	
	@Override
	public void onEnable() {
		
		new Metrics(this, 7571);
		
		portalHandler = new PortalHandler(this);
		viewingHandler = new ViewingHandler(this, portalHandler);
		
		loadConfig();
		loadConfigData();
		registerListeners();
	}
	
	@Override
	public void onDisable() {
		viewingHandler.reset();
	}
	
	private void reload() {
		
		portalHandler.reset();
		viewingHandler.reset();
		
		loadConfig();
		loadConfigData();
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
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if ("nvreload".equals(command.getName())) {
			
			if (sender.hasPermission(RELOAD_PERM)) {
				reload();
				sender.sendMessage(ChatColor.DARK_RED + "[" + ChatColor.DARK_PURPLE + "NV" + ChatColor.DARK_RED + "]" + ChatColor.LIGHT_PURPLE + " Reloaded config settings.");
				
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have the permission for this command!");
			}
			
			return true;
		}
		return false;
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
		
		worldsWithProjectingPortals = new HashSet<>();
		viewableOnlyWorlds = new HashSet<>();
		
		List<String> worldNames = getConfig().getStringList("worlds-with-projecting-portals");
		
		for (String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().info("Could not find world " + worldName + ".");
			} else {
				worldsWithProjectingPortals.add(world.getUID());
			}
		}
		
		worldNames = getConfig().getStringList("viewable-worlds");
		
		for (String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if (world == null) {
				getLogger().info("Could not find world " + worldName + ".");
			} else {
				viewableOnlyWorlds.add(world.getUID());
			}
		}
	}
	
	public void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(this, portalHandler), this);
		manager.registerEvents(new PlayerMoveListener(this, viewingHandler), this);
		manager.registerEvents(new BlockListener(this, portalHandler, viewingHandler), this);
	}
}