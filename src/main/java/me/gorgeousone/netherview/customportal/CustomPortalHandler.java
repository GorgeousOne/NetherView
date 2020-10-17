package me.gorgeousone.netherview.customportal;


import me.gorgeousone.netherview.geometry.BlockVec;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomPortalHandler {
	
	private final Map<String, CustomPortal> customPortals;
	private final Map<UUID, Set<CustomPortal>> worldsWithCustomPortals;
	
	public CustomPortalHandler() {
		this.worldsWithCustomPortals = new HashMap<>();
		this.customPortals = new HashMap<>();
	}
	
	public CustomPortal getPortal(String portalName) {
		return customPortals.get(portalName);
	}
	
	public void addPortal(CustomPortal portal) {
		customPortals.put(portal.getName(), portal);
		
		UUID worldId = portal.getWorld().getUID();
		worldsWithCustomPortals.computeIfAbsent(worldId, set -> new HashSet<>());
		worldsWithCustomPortals.get(worldId).add(portal);
	}
	
	public void removePortal(CustomPortal portal) {
		customPortals.remove(portal.getName());
		worldsWithCustomPortals.get(portal.getWorld().getUID()).remove(portal);
	}
	
	public Set<String> getPortalNames() {
		return customPortals.keySet();
	}
	
	public Set<CustomPortal> getCustomPortals(World world) {
		return worldsWithCustomPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public CustomPortal getPortal(Location location) {
		
		for (CustomPortal portal : getCustomPortals(location.getWorld())) {
			
			if (portal.getInner().contains(new BlockVec(location))) {
				return portal;
			}
		}
		
		return null;
	}
	
	public boolean isValidName(String portalName) {
		return portalName.matches("^(?=.{1,32}$)[a-z0-9_-]+");
	}
	
	public boolean isUniqueName(String portalName) {
		return !customPortals.containsKey(portalName);
	}
	
	public String createGenericPortalName() {
		
		for (int i = 1; i <= 10000; ++i) {
			
			String genericName = "portal" + i;
			
			if (isUniqueName(genericName)) {
				return genericName;
			}
		}
		
		return "there is no way you created over 10,000 custom portals";
	}
}