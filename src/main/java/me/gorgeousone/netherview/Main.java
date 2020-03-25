package me.gorgeousone.netherview;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	
	@Override
	public void onEnable() {
		registerListeners();
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
	
	public void registerListeners() {
		
		PluginManager manager = Bukkit.getPluginManager();
		manager.registerEvents(new PortalListener(null), this);
	}
}
