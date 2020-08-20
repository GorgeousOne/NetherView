package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {
	
	public static boolean debugMessagesEnabled;
	public static boolean warningMessagesEnabled;
	
	public static void setWarningMessagesEnabled(boolean warningMessagesEnabled) {
		MessageUtils.warningMessagesEnabled = warningMessagesEnabled;
	}
	
	public static void setDebugMessagesEnabled(boolean debugMessagesEnabled) {
		MessageUtils.debugMessagesEnabled = debugMessagesEnabled;
	}
	
	public static void sendWarning(Player player, String message) {
		
		if (warningMessagesEnabled) {
			player.sendMessage(message);
		}
	}
	
	public static void printDebug(String message) {
		
		if (debugMessagesEnabled) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] " + message);
		}
	}
}
