package me.gorgeousone.netherview.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ConfigUtils {
	
	private ConfigUtils() {}
	
	public static YamlConfiguration loadConfig(String configName, JavaPlugin plugin) {
		
		File configFile = new File(plugin.getDataFolder() + File.separator + configName + ".yml");
		YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(configName + ".yml")));
		
		if (!configFile.exists()) {
			plugin.saveResource(configName + ".yml", true);
		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		config.setDefaults(defConfig);
		config.options().copyDefaults(true);
		
		try {
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return config;
	}

//	public static YamlConfiguration loadDefaultConfig(String configName, JavaPlugin plugin) {
//
//		InputStream defConfigStream = plugin.getResource(configName + ".yml");
//		return YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
//	}
}