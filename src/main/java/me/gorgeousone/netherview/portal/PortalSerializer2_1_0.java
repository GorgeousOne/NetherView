package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.MessageException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class PortalSerializer2_1_0 {
	
	private final JavaPlugin plugin;
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	
	public PortalSerializer2_1_0(JavaPlugin plugin,
	                             ConfigSettings configSettings,
	                             PortalHandler portalHandler) {
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
		this.plugin = plugin;
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
		
		loadPortalLocations(portalConfig);
		loadPortalData(portalConfig);
	}
	
	private void loadPortalLocations(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-locations")) {
			return;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		for (String worldID : portalLocations.getKeys(false)) {
			
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				plugin.getLogger().warning("Could not find world with ID: '" + worldID + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (configSettings.canCreatePortalViews(worldWithPortals)) {
				deserializePortals(worldWithPortals, portalLocations.getStringList(worldID));
			}
		}
	}
	
	private void deserializePortals(World world, List<String> portalLocs) {
		
		for (String serializedBlockVec : portalLocs) {
			
			try {
				BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
				Portal portal = PortalLocator.locatePortalStructure(portalLoc.toBlock(world));
				portalHandler.addPortal(portal);
				
			} catch (IllegalArgumentException | IllegalStateException | MessageException e) {
				plugin.getLogger().warning("Unable to load portal at [" + world.getName() + "," + serializedBlockVec + "]: " + e.getMessage());
			}
		}
	}
	
	private void loadPortalData(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-data")) {
			return;
		}
		
		ConfigurationSection portalData = portalConfig.getConfigurationSection("portal-data");
		
		for (String portalHashString : portalData.getKeys(false)) {
			
			Portal portal = portalHandler.getPortalByHash(Integer.parseInt(portalHashString));
			
			if (portal == null) {
				continue;
			}
			
			portal.setViewFlipped(portalData.getBoolean(portalHashString + ".is-flipped"));
			
			if (!portalData.contains(portalHashString + ".link")) {
				continue;
			}
			
			int linkedPortalHash = portalData.getInt(portalHashString + ".link");
			Portal counterPortal = portalHandler.getPortalByHash(linkedPortalHash);
			
			if (counterPortal != null) {
				
				try {
					portalHandler.linkPortalTo(portal, counterPortal, null);
				} catch (MessageException e) {
					plugin.getLogger().warning("Could not link portal '" + portal.toString() + "' to portal '" + counterPortal.toString() + "': " + e.getMessage());
				}
			}
		}
	}
}