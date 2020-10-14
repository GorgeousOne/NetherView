package me.gorgeousone.netherview;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public enum Message {
	
	SUCCESSFUL_PORTAL_LINKING("successful-portal-linking"),
	UNEQUAL_PORTALS("unequal-portals"),
	PORTAL_FRAME_INCOMPLETE("portal-frame-incomplete", "world-type"),
	PORTAL_CORNERS_INCOMPLETE("portal-corners-incomplete", "world-type"),
	PORTAL_TOO_BIG("portal-too-big", "size"),
	PORTAL_NOT_INTACT("portal-not-intact", "world-type"),
	WORLD_NOT_WHITE_LISTED("world-not-white-listed", "world"),
	NO_WORLD_FOUND("no-world-found", "world"),
	NO_PORTALS_FOUND("no-portals-found", "world"),
	NO_PORTAL_FOUND_NEARBY("no-portal-found-nearby"),
	PORTAL_INFO("portal-info", "location", "is-flipped", "counter-portal", "linked-portals"),
	WORLD_INFO("world-info", "count", "world", "portals"),
	FLIPPED_PORTAL("flipped-portal", "portal"),
	PORTAL_VIEWING_ON("portal-viewing-on"),
	PORTAL_VIEWING_OFF("portal-viewing-off"),
	
	SET_FIRST_CUBOID_POSITION("set-first-position", "position"),
	SET_SECOND_CUBOID_POSITION("set-second-position", "position"),
	SELECTION_TOO_SMALL("selection-too-small"),
	SELECTION_NOT_FLAT("selection-not-flat"),
	NO_PORTAL_FOUND_WITH_NAME("no-portal-found-with-name", "name"),
	PORTALS_INTERSECT("portals-intersect"),
	PORTAL_NAME_NOT_UNIQUE("portal-name-not-unique", "name"),
	PORTAL_CREATED("portal-created", "name", "size"),
	PORTALS_LINKED("portals-linked", "portal1", "portal2")
	;
	
	private final String configKey;
	private String configValue;
	private final String[] placeholdersTokens;
	
	Message(String configKey, String... placeholdersTokens) {
		this.configKey = configKey;
		this.placeholdersTokens = placeholdersTokens;
	}
	
	private String getConfigKey() {
		return configKey;
	}
	
	public String[] getPlaceholdersTokens() {
		return placeholdersTokens;
	}
	
	public String[] getMessage(String... placeholderValues) {
		
		if (placeholderValues.length != placeholdersTokens.length) {
			throw new IllegalArgumentException("Expected " + placeholdersTokens.length + " placeholder values, found " + placeholderValues.length);
		}
		
		String formattedMessage = configValue;
		
		for (int i = 0; i < placeholdersTokens.length; ++i) {
			formattedMessage = formattedMessage.replace("%" + placeholdersTokens[i] + "%", placeholderValues[i]);
		}
		
		return ChatColor.translateAlternateColorCodes('&', formattedMessage).split("\\\\n");
	}
	
	public static void loadLangConfigValues(FileConfiguration langConfig) {
		
		for (Message message : Message.values()) {
			
			String configKey = message.getConfigKey();
			
			if (!langConfig.contains(configKey)) {
				throw new IllegalArgumentException("Language config does not contain key '" + configKey + "' for message " + message + ".");
			}
			
			message.configValue = langConfig.getString(configKey);
		}
	}
}
