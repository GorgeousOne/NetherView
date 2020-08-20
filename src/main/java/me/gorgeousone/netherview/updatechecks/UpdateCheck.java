package me.gorgeousone.netherview.updatechecks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.BiConsumer;

public class UpdateCheck {
	
	private final JavaPlugin javaPlugin;
	
	private final String currentVersion;
	private final int resourceId;
	private BiConsumer<VersionResponse, String> versionResponse;
	
	public UpdateCheck(JavaPlugin javaPlugin, int resourceId) {
		
		this.javaPlugin = Objects.requireNonNull(javaPlugin, "javaPlugin");
		this.currentVersion = javaPlugin.getDescription().getVersion();
		this.resourceId = resourceId;
	}
	
	public UpdateCheck handleResponse(BiConsumer<VersionResponse, String> versionResponse) {
		this.versionResponse = versionResponse;
		return this;
	}
	
	public void check() {
		
		Objects.requireNonNull(versionResponse, "versionResponse");
		
		Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> {
			
			try {
				InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openStream();
				Scanner scanner = new Scanner(inputStream);
				
				if (!scanner.hasNext()) {
					return;
				}
				
				String fetchedVersion = scanner.next();
				VersionResponse response = compareVersionStrings(fetchedVersion);
				
				Bukkit.getScheduler().runTask(javaPlugin, () -> versionResponse.accept(
						response, response == VersionResponse.FOUND_NEW ? fetchedVersion : currentVersion));
				
			} catch (IOException exception) {
				versionResponse.accept(VersionResponse.UNAVAILABLE, null);
			}
		});
	}
	
	private VersionResponse compareVersionStrings(String fetchedVersion) {
		
		if (fetchedVersion.equals(currentVersion)) {
			return VersionResponse.LATEST;
		}
		
		String[] currentDigits = currentVersion.split("\\.");
		String[] fetchedDigits = fetchedVersion.split("\\.");
		
		int minDigitCount = Math.min(currentDigits.length, fetchedDigits.length);
		
		try {
			
			for (int i = 0; i < minDigitCount; i++) {
				
				int currentDigit = Integer.parseInt(currentDigits[i]);
				int fetchedDigit = Integer.parseInt(fetchedDigits[i]);
				
				if (fetchedDigit > currentDigit) {
					return VersionResponse.FOUND_NEW;
				} else if (fetchedDigit < currentDigit) {
					return VersionResponse.LATEST;
				}
			}
			
		} catch (NumberFormatException e) {
			return VersionResponse.LATEST;
		}
		
		return VersionResponse.FOUND_NEW;
	}
}