package me.gorgeousone.netherview;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigUtils {
	
	private ConfigUtils() {}
	
	public static YamlConfiguration loadConfig(String configName, JavaPlugin plugin) {
		
		File configFile = new File(plugin.getDataFolder() + File.separator + configName + ".yml");
		
		if (!configFile.exists()) {
			configFile.mkdir();
		}
		
		return YamlConfiguration.loadConfiguration(configFile);
	}
}