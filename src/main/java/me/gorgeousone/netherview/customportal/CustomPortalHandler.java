package me.gorgeousone.netherview.customportal;


import java.util.HashMap;
import java.util.Map;

public class CustomPortalHandler {
	
	private final Map<String, CustomPortal> customPortals;
	
	public CustomPortalHandler() {
		this.customPortals = new HashMap<>();
	}
	
	public void addPortal(CustomPortal portal) {
		customPortals.put(portal.getName(), portal);
	}
	
	public boolean isValidName(String portalName) {
		return portalName.matches("^(?=.{1,32}$)[a-z0-9_-]+");
	}
	
	public boolean isUniqueName(String portalName) {
		return !customPortals.containsKey(portalName);
	}
	
	public CustomPortal getPortal(String portalName) {
		return customPortals.get(portalName);
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
