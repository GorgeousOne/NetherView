package me.gorgeousone.netherview;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.listeners.MapCreator;
import me.gorgeousone.netherview.listeners.TeleportListener;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	@Override
	public void onEnable() {
	
		portalHandler = new PortalHandler();
		viewingHandler = new ViewingHandler(portalHandler);
		
		registerListeners();
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
	
	public void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new TeleportListener(portalHandler), this);
		manager.registerEvents(new MapCreator(), this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (!(sender instanceof Player))
			return false;
		
		String cmdName = command.getName();
		
		if ("viewportal".equals(cmdName)) {
			displayNearestPortal((Player) sender);
			return true;
		}
		
		return false;
	}
	
	private void displayNearestPortal(Player player) {
		
		Location playerLoc = player.getEyeLocation();
		
		Portal portal = portalHandler.getNearestPortal(playerLoc);
		
		if (portal != null)
			viewingHandler.displayPortal(player, portal);
	}
}