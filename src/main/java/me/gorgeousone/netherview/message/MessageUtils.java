package me.gorgeousone.netherview.message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtils {
	
	private MessageUtils() {}
	
	public static boolean debugMessagesEnabled;
	public static boolean warningMessagesEnabled;
	
	public static void setWarningMessagesEnabled(boolean warningMessagesEnabled) {
		MessageUtils.warningMessagesEnabled = warningMessagesEnabled;
	}
	
	public static void setDebugMessagesEnabled(boolean debugMessagesEnabled) {
		MessageUtils.debugMessagesEnabled = debugMessagesEnabled;
	}
	
	public static void sendInfo(CommandSender sender, Message message, String... placeholderValues) {
		sender.sendMessage(message.getMessage(placeholderValues));
	}
	
	public static void sendWarning(CommandSender sender, Message message, String... placeholderValues) {
		
		if (warningMessagesEnabled) {
			sender.sendMessage(message.getMessage(placeholderValues));
		}
	}
	
	public static void printDebug(String message) {
		
		if (debugMessagesEnabled) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] " + message);
		}
	}
}
