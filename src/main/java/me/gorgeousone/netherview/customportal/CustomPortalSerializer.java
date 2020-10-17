package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomPortalSerializer {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	private final CustomPortalHandler customPortalHandler;
	
	public CustomPortalSerializer(JavaPlugin plugin,
	                              ConfigSettings configSettings,
	                              PortalHandler portalHandler,
	                              CustomPortalHandler customPortalHandler) {
		
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
		this.plugin = plugin;
		this.customPortalHandler = customPortalHandler;
	}
	
	public void savePortals(FileConfiguration customPortalConfig) {
		
		customPortalConfig.set("plugin-version", plugin.getDescription().getVersion());
		customPortalConfig.set("custom-portals", null);
		
		ConfigurationSection portalSection = customPortalConfig.createSection("custom-portals");
		
		for (World world : Bukkit.getWorlds()) {
			
			if (!portalHandler.hasPortals(world)) {
				continue;
			}
			
			Set<Portal> portalsInWorld = portalHandler.getPortals(world);
			String worldId = world.getUID().toString();
			ConfigurationSection worldSection = null;
			
			for (Portal portal : portalsInWorld) {
				
				if (!(portal instanceof CustomPortal)) {
					continue;
				}
				
				if (worldSection == null) {
					worldSection = portalSection.createSection(worldId);
				}
				
				CustomPortal customPortal = (CustomPortal) portal;
				String portalName = customPortal.getName();
				Cuboid portalFrame = customPortal.getFrame();
				ConfigurationSection portalData = worldSection.createSection(portalName);
				
				portalData.set("min", portalFrame.getMin().serialize());
				portalData.set("max", portalFrame.getMax().serialize());
				portalData.set("isFlipped", portal.isViewFlipped());
				
				if (customPortal.isLinked()) {
					portalData.set("link", ((CustomPortal) customPortal.getCounterPortal()).getName());
				}
			}
		}
	}
	
	public void loadPortals(FileConfiguration customPortalConfig) {
		
		if (!customPortalConfig.contains("custom-portals")) {
			return;
		}
		
		ConfigurationSection portalsSection = customPortalConfig.getConfigurationSection("custom-portals");
		Map<String, String> portalLinks = new HashMap<>();
		
		for (String worldId : portalsSection.getKeys(false)) {
			
			World world = Bukkit.getWorld(UUID.fromString(worldId));
			
			if (world == null) {
				plugin.getLogger().warning("Could not find world with ID: '" + worldId + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (configSettings.canCreateCustomPortals(world)) {
				
				ConfigurationSection worldSection = portalsSection.getConfigurationSection(worldId);
				
				for (String portalName : worldSection.getKeys(false)) {
					deserializeCustomPortal(world, portalName, worldSection.getConfigurationSection(portalName), portalLinks);
				}
			}
		}
		
		linkCustomPortals(portalLinks);
	}
	
	private void deserializeCustomPortal(World world,
	                                     String portalName,
	                                     ConfigurationSection portalData,
	                                     Map<String, String> portalLinks) {
		
		try {
			BlockVec portalMin = BlockVec.fromString(portalData.getString("min"));
			BlockVec portalMax = BlockVec.fromString(portalData.getString("max"));
			
			Cuboid portalFrame = new Cuboid(portalMin, portalMax);
			CustomPortal portal = CustomPortalCreator.createPortal(world, portalFrame);
			
			portal.setName(portalName);
			portal.setViewFlipped(portalData.getBoolean("is-flipped"));
			
			customPortalHandler.addPortal(portal);
			portalHandler.addPortal(portal);
			
			if (portalData.contains("link")) {
				portalLinks.put(portalName, portalData.getString("link"));
			}
			
		} catch (IllegalArgumentException | IllegalStateException | MessageException e) {
			plugin.getLogger().warning("Unable to load custom portal at [" + world.getName() + "," + portalName + "]: " + e.getMessage());
		}
	}
	
	private void linkCustomPortals(Map<String, String> portalLinks) {
		
		for (Map.Entry<String, String> entry : portalLinks.entrySet()) {
			
			String fromName = entry.getKey();
			String toName = entry.getValue();
			
			CustomPortal fromPortal = customPortalHandler.getPortal(entry.getKey());
			CustomPortal toPortal = customPortalHandler.getPortal(entry.getValue());
			
			if (toPortal == null) {
				plugin.getLogger().warning("Could not find custom portal with name'" + toName + "' for linking with portal '" + fromName + "'.");
				continue;
			}
			
			try {
				portalHandler.linkPortalTo(fromPortal, toPortal, null);
				
			} catch (MessageException e) {
				plugin.getLogger().warning("Unable to link custom portal '" + fromName + "' to portal '" + toName + "': " + e.getMessage());
			}
		}
	}
}
