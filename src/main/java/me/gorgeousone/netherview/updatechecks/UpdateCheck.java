package me.gorgeousone.netherview.updatechecks;

import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;   

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UpdateCheck {
	
	private final JavaPlugin plugin;
	
	private final String currentVersion;
	private final int resourceId;
	private final String resourceName;
	private final String updateInfoPasteUrl;
	
	public UpdateCheck(JavaPlugin plugin, int resourceId, String resourceName, String updateInfoPasteUrl) {
		
		this.plugin = plugin;
		this.currentVersion = plugin.getDescription().getVersion();
		this.resourceId = resourceId;
		this.resourceName = resourceName;
		this.updateInfoPasteUrl = updateInfoPasteUrl;
	}
	
	public void run(int maxDisplayedMessages) {
		
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			
			try {
				List<UpdateInfo> newUpdates = readNewUpdates();
				
				if (newUpdates.isEmpty()) {
					plugin.getLogger().info("Plugin is up to date :)");
					return;
				}
				
				if (newUpdates.size() < 2) {
					MessageUtils.sendStaffInfo("A new version of Nether View is available!");
				}else {
					MessageUtils.sendStaffInfo("New updates for Nether View are available!");
				}
				
				for (int i = 0; i < newUpdates.size(); ++i) {
					
					if (i > maxDisplayedMessages - 1) {
						ComponentBuilder message = new ComponentBuilder((newUpdates.size() - maxDisplayedMessages) + " more...").color(ChatColor.LIGHT_PURPLE);
						message.event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/" + resourceName + "." + resourceId + "/updates"));
						message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("see all updates").create()));
						MessageUtils.sendStaffInfo(message.create());
						break;
					}
					
					MessageUtils.sendStaffInfo(newUpdates.get(i).getChatMessage());
				}
				
				ComponentBuilder builder = new ComponentBuilder("download").color(ChatColor.YELLOW).underlined(true);
				builder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/" + resourceName + "." + resourceId));
				builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("visit ").append("download page").color(ChatColor.LIGHT_PURPLE).create()));
				MessageUtils.sendStaffInfo(builder.create());
				
			} catch (IOException exception) {
				plugin.getLogger().info("Unable to check for updates...");
			}
		});
	}
	
	private List<UpdateInfo> readNewUpdates() throws IOException {
		
		InputStream inputStream = new URL(updateInfoPasteUrl).openStream();
		Scanner scanner = new Scanner(inputStream);
		List<UpdateInfo> updates = new ArrayList<>();
		
		while (scanner.hasNext()) {
			
			UpdateInfo updateInfo = new UpdateInfo(scanner.nextLine(), resourceName, resourceId);
			
			if (VersionUtils.isVersionLowerThan(currentVersion, updateInfo.getVersion())) {
				System.out.println(currentVersion + " is lower than " + updateInfo.getVersion());
				updates.add(updateInfo);
			}else {
				break;
			}
		}
		return updates;
	}
}