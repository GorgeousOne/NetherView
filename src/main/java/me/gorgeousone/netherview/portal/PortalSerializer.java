package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.utils.MessageException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PortalSerializer {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	
	public PortalSerializer(NetherViewPlugin main, PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
		this.main = main;
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		portalConfig.set("plugin-version", main.getDescription().getVersion());
		portalConfig.set("portal-locations", null);
		portalConfig.set("portal-data", null);
		
		ConfigurationSection portalLocations = portalConfig.createSection("portal-locations");
		ConfigurationSection portalData = portalConfig.createSection("portal-data");
		
		for (World world : Bukkit.getWorlds()) {
			
			if (!portalHandler.hasPortals(world)) {
				continue;
			}
			
			Set<Portal> portalsInWorld = portalHandler.getPortals(world);
			List<String> portalStrings = new ArrayList<>();
			
			for (Portal portal : portalsInWorld) {
				
				if (portal.getType() == PortalType.CUSTOM) {
					continue;
				}
				
				int portalHash = portal.hashCode();
				
				portalStrings.add(new BlockVec(portal.getLocation()).toString());
				portalData.set(portalHash + ".is-flipped", portal.isViewFlipped());
				
				if (portal.isLinked()) {
					portalData.set(portalHash + ".link", portal.getCounterPortal().hashCode());
				}
			}
			
			portalLocations.set(world.getUID().toString(), portalStrings);
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) throws MessageException {
		
		loadPortalLocations(main, portalConfig);
		loadPortalData(portalConfig);
	}
	
	private void loadPortalLocations(NetherViewPlugin main, FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-locations")) {
			return;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		for (String worldID : portalLocations.getKeys(false)) {
			
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				main.getLogger().warning("Could not find world with ID: '" + worldID + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (main.canCreatePortalViews(worldWithPortals)) {
				deserializePortals(worldWithPortals, portalLocations.getStringList(worldID));
			}
		}
	}
	
	private void deserializePortals(World world, List<String> portalLocs) {
		
		for (String serializedBlockVec : portalLocs) {
			
			try {
				BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
				portalHandler.addPortalStructure(world.getBlockAt(portalLoc.getX(), portalLoc.getY(), portalLoc.getZ()));
				
			} catch (IllegalArgumentException | IllegalStateException | MessageException e) {
				throw new IllegalArgumentException("Unable to load portal at [" + world.getName() + ", " + serializedBlockVec + "]: " + e.getMessage());
			}
		}
	}
	
	private void loadPortalData(FileConfiguration portalConfig) throws MessageException {
		
		if (!portalConfig.contains("portal-data")) {
			return;
		}
		
		ConfigurationSection portalData = portalConfig.getConfigurationSection("portal-data");
		
		for (String portalHashString : portalData.getKeys(false)) {
			
			Portal portal = portalHandler.getPortalByHashCode(Integer.parseInt(portalHashString));
			
			if (portal == null) {
				continue;
			}
			
			portal.setViewFlipped(portalData.getBoolean(portalHashString + ".is-flipped"));
			
			if (!portalData.contains(portalHashString + ".link")) {
				continue;
			}
			
			int linkedPortalHash = portalData.getInt(portalHashString + ".link");
			Portal counterPortal = portalHandler.getPortalByHashCode(linkedPortalHash);
			
			if (counterPortal != null) {
				portalHandler.linkPortalTo(portal, counterPortal, null);
			}
		}
	}
}