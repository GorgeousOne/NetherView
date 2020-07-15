package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class ConsoleUtils {
	
	public static boolean debugMessagesEnabled;
	
	private ConsoleUtils() {}
	
	public static void setDebugMessagesEnabled(boolean debugMessagesEnabled) {
		ConsoleUtils.debugMessagesEnabled = debugMessagesEnabled;
	}
	
	public static void printDebug(String message) {
		
		if (debugMessagesEnabled) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] " + message);
		}
	}
}
