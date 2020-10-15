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
				
				worldSection.set(portalName + ".min", portalFrame.getMin().serialize());
				worldSection.set(portalName + ".max", portalFrame.getMax().serialize());
				worldSection.set(portalName + ".isFlipped", portal.isViewFlipped());
				
				if (customPortal.isLinked()) {
					worldSection.set(portalName + ".link", ((CustomPortal) customPortal.getCounterPortal()).getName());
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
			
			ConfigurationSection worldSection = portalsSection.getConfigurationSection(worldId);
			
			for (String portalName : worldSection.getKeys(false)) {
				
				ConfigurationSection portalData = worldSection.getConfigurationSection(portalName);
				
				try {
					
					BlockVec portalMin = BlockVec.fromString(portalData.getString("min"));
					BlockVec portalMax = BlockVec.fromString(portalData.getString("max"));
					
					Cuboid portalFrame = new Cuboid(portalMin, portalMax);
					CustomPortal portal = CustomPortalCreator.createPortal(world, portalFrame);
					
					portal.setName(portalName);
					portal.setViewFlipped(portalData.getBoolean("is-flipped"));
					
					if (portalData.contains("link")) {
						portalLinks.put(portalName, portalData.getString("link"));
					}
					
					customPortalHandler.addPortal(portal);
					
					if (configSettings.canCreateCustomPortals(world)) {
						portalHandler.addPortal(portal);
					}
					
				} catch (IllegalArgumentException | IllegalStateException | MessageException e) {
					throw new IllegalArgumentException("Unable to load portal at [" + world.getName() + "," + portalName + "]: " + e.getMessage());
				}
			}
		}
		
		linkCustomPortals(portalLinks);
	}
	
	private void linkCustomPortals(Map<String, String> portalLinks) {
		
		for (Map.Entry<String, String> entry : portalLinks.entrySet()) {
			
			String fromName = entry.getKey();
			String toName = entry.getValue();
			
			CustomPortal fromPortal = customPortalHandler.getPortal(entry.getKey());
			CustomPortal toPortal = customPortalHandler.getPortal(entry.getValue());
			
			if (toPortal == null) {
				plugin.getLogger().warning("Could not find portal with name'" + toName + "' for linking with portal '" + fromName + "'.");
				continue;
			}
			
			try {
				portalHandler.linkPortalTo(fromPortal, toPortal, null);
				
			}catch (MessageException e) {
				plugin.getLogger().warning("Unable to link portal '" + fromName + "' to portal '" + toName + "': " + e.getMessage());
			}
		}
	}
}
