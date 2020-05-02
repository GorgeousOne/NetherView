package me.gorgeousone.netherview;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.listeners.BlockListener;
import me.gorgeousone.netherview.listeners.MapCreator;
import me.gorgeousone.netherview.listeners.PlayerMoveListener;
import me.gorgeousone.netherview.listeners.TeleportListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin {
	
	public final static String VIEW_PERM = "netherview.viewportals";
	public final static String LINK_PERM = "netherview.linkportals";
	public final static String RELOAD_PERM = "netherview.reload";
	
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	private Set<UUID> worldsWithProejctingPortals;
	private Set<UUID> viewableOnlyWorlds;
	
	private boolean hidePortalBlocks;
	private int portalViewDist;
	
	@Override
	public void onEnable() {
	
		portalHandler = new PortalHandler();
		viewingHandler = new ViewingHandler(this, portalHandler);
		
		loadConfig();
		loadConfigData();
		registerListeners();
		
		System.out.println("is block " + Material.GLOWSTONE.isBlock());
		System.out.println("is solid " + Material.GLOWSTONE.isSolid());
		System.out.println("is occluding " + Material.GLOWSTONE.isOccluding());
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
	
	public boolean hidePortalBlocks() {
		return hidePortalBlocks;
	}
	
	public boolean canWorldViewOtherWorlds(World world) {
		return worldsWithProejctingPortals.contains(world.getUID());
	}
	
	public boolean canBeViewed(World world) {
		return viewableOnlyWorlds.contains(world.getUID());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if("nvreload".equals(command.getName())) {
			
			if (sender.hasPermission(RELOAD_PERM)) {
				reload();
				sender.sendMessage(ChatColor.DARK_RED + "[" + ChatColor.DARK_PURPLE + "NV" + ChatColor.DARK_RED + "]" + ChatColor.LIGHT_PURPLE + " Reloaded.");
				
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
		
		hidePortalBlocks = getConfig().getBoolean("hide-portal-blocks", true);
		portalViewDist = getConfig().getInt("portal-view-distance-in-blocks", 8);
		
		worldsWithProejctingPortals = new HashSet<>();
		viewableOnlyWorlds = new HashSet<>();

		List<String> worldNames = getConfig().getStringList("worlds-with-projecting-portals");
		
		for(String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if(world == null) {
				getLogger().info("Could not find world " + worldName + ".");
			}else {
				worldsWithProejctingPortals.add(world.getUID());
			}
		}
		
		worldNames = getConfig().getStringList("viewable-worlds");
		
		for(String worldName : worldNames) {
			World world = Bukkit.getWorld(worldName);
			
			if(world == null) {
				getLogger().info("Could not find world " + worldName + ".");
			}else {
				viewableOnlyWorlds.add(world.getUID());
			}
		}
	}
	
	public void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(this, portalHandler), this);
		manager.registerEvents(new PlayerMoveListener(this, viewingHandler), this);
		manager.registerEvents(new MapCreator(), this);
		manager.registerEvents(new BlockListener(this, portalHandler), this);
	}
}