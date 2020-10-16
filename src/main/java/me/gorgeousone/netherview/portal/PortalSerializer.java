package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalSerializer {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	
	public PortalSerializer(JavaPlugin plugin,
	                        ConfigSettings configSettings,
	                        PortalHandler portalHandler) {
		
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
		this.plugin = plugin;
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		portalConfig.set("plugin-version", plugin.getDescription().getVersion());
		portalConfig.set("portals", null);
		
		ConfigurationSection portalSection = portalConfig.createSection("portals");
		
		for (World world : Bukkit.getWorlds()) {
			
			if (!portalHandler.hasPortals(world)) {
				continue;
			}
			
			Set<Portal> portalsInWorld = portalHandler.getPortals(world);
			String worldId = world.getUID().toString();
			ConfigurationSection worldSection = null;
			
			for (Portal portal : portalsInWorld) {
				
				if (portal instanceof CustomPortal) {
					continue;
				}
				
				if (worldSection == null) {
					worldSection = portalSection.createSection(worldId);
				}
				
				int portalHash = portal.hashCode();
				ConfigurationSection portalData = worldSection.createSection(Integer.toString(portalHash));
				
				portalData.set("location", new BlockVec(portal.getLocation()).serialize());
				portalData.set("isFlipped", portal.isViewFlipped());
				
				if (portal.isLinked()) {
					portalData.set("link", portal.getCounterPortal().hashCode());
				}
			}
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("plugin-version") || VersionUtils.versionIsLowerThan(portalConfig.getString("plugin-version"), "3")) {
			new PortalSerializer2_1_0(plugin, configSettings, portalHandler).loadPortals(portalConfig);
			return;
		}
		
		if (!portalConfig.contains("portals")) {
			return;
		}
		
		ConfigurationSection portalsSection = portalConfig.getConfigurationSection("portals");
		Map<Integer, Integer> portalLinks = new HashMap<>();
		
		for (String worldId : portalsSection.getKeys(false)) {
			
			World world = Bukkit.getWorld(UUID.fromString(worldId));
			
			if (world == null) {
				plugin.getLogger().warning("Could not find world with ID: '" + worldId + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (configSettings.canCreatePortalViews(world)) {
				
				ConfigurationSection worldSection = portalsSection.getConfigurationSection(worldId);
				
				for (String portalHashString : worldSection.getKeys(false)) {
					deserializePortal(world, portalHashString, worldSection.getConfigurationSection(portalHashString), portalLinks);
				}
			}
		}
		
		linkPortals(portalLinks);
	}
	
	private void deserializePortal(World world,
	                               String portalHashString,
	                               ConfigurationSection portalData,
	                               Map<Integer, Integer> portalLinks) {
		
		try {
			
			int portalHash = Integer.parseInt(portalHashString);
			
			BlockVec portalLoc = BlockVec.fromString(portalData.getString("location"));
			Portal portal = PortalLocator.locatePortalStructure(portalLoc.toBlock(world));
			portalHandler.addPortal(portal);
			portal.setViewFlipped(portalData.getBoolean("is-flipped"));
			
			if (portalData.contains("link")) {
				portalLinks.put(portalHash, portalData.getInt("link"));
			}
			
		} catch (IllegalArgumentException | IllegalStateException | MessageException e) {
			plugin.getLogger().warning("Unable to load portal '" + portalHashString + "' in world " + world.getName() + "': " + e.getMessage());
		}
	}
	
	private void linkPortals(Map<Integer, Integer> portalLinks) {
		
		for (Map.Entry<Integer, Integer> entry : portalLinks.entrySet()) {
			
			Integer fromPortalHash = entry.getKey();
			Integer toPortalHash = entry.getValue();
			
			Portal fromPortal = portalHandler.getPortalByHash(fromPortalHash);
			Portal toPortal = portalHandler.getPortalByHash(toPortalHash);
			
			if (toPortal == null) {
				plugin.getLogger().warning("Could not find custom portal with name'" + toPortalHash + "' for linking with portal '" + fromPortalHash + "'.");
				continue;
			}
			
			try {
				portalHandler.linkPortalTo(fromPortal, toPortal, null);
				
			} catch (MessageException e) {
				plugin.getLogger().warning("Unable to link custom portal '" + fromPortalHash + "' to portal '" + toPortalHash + "': " + e.getMessage());
			}
		}
	}
}
