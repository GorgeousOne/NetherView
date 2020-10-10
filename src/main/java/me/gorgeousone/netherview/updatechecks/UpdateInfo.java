package me.gorgeousone.netherview.updatechecks;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class UpdateInfo {
	
	private final String version;
	private final String description;
	private final String updateId;
	
	private final int resourceId;
	private final String resourceName;
	
	public UpdateInfo(String updateInfo, String resourceName, int resourceId) {
	
		String[] split = updateInfo.split(";");
		version = split[0];
		description = split[1];
		updateId = split[2];
		
		this.resourceName = resourceName.toLowerCase().replace(' ', '-');
		this.resourceId = resourceId;
	}
	
	public String getVersion() {
		return version;
	}
	
	public BaseComponent[] getChatMessage() {
		
		ComponentBuilder builder = new ComponentBuilder(version).color(ChatColor.LIGHT_PURPLE);
		builder.append(" " + description).color(ChatColor.YELLOW);
		builder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/" + resourceName + "." + resourceId + "/update?update=" + updateId));
		builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("read more about ").append(version).color(ChatColor.LIGHT_PURPLE).create()));
		
		return builder.create();
	}
}
