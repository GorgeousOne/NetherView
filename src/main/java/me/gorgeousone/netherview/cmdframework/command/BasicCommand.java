package me.gorgeousone.netherview.cmdframework.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This is the beginning of a lot of unnecessary code.
 * I mean it is kind of useful because I can create child commands, aliases, number inputs and tab lists on the go but
 * yeah it feels somehow unnecessary that this "api" is like at least 10 pages extra code.
 * But it's more soft coded so it's also cool in a way.
 */
public abstract class BasicCommand {
	
	private String name;
	private String permission;
	private boolean isPlayerRequired;
	
	private Set<String> aliases;
	private ParentCommand parent;
	
	protected BasicCommand(String name, String permission, boolean isPlayerRequired) {
		this(name, permission, isPlayerRequired, null);
	}
	
	protected BasicCommand(String name, String permission, boolean isPlayerRequired, ParentCommand parent) {
		
		this.name = name;
		this.permission = permission;
		this.isPlayerRequired = isPlayerRequired;
		this.parent = parent;
		
		aliases = new HashSet<>();
		aliases.add(name);
	}
	
	public String getName() {
		return name;
	}
	
	public String getPermission() {
		return permission;
	}
	
	public boolean isPlayerRequired() {
		return isPlayerRequired;
	}
	
	public ParentCommand getParent() {
		return parent;
	}
	
	public boolean isChild() {
		return parent != null;
	}
	
	public boolean matches(String alias) {
		return aliases.contains(alias);
	}
	
	protected void addAlias(String alias) {
		aliases.add(alias);
	}
	
	public void execute(CommandSender sender, String[] arguments) {
		
		if (isPlayerRequired && !(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
			return;
		}
		
		if (permission != null && !sender.hasPermission(getPermission())) {
			sender.sendMessage(ChatColor.RED + "You do not have the permission for this command.");
			return;
		}
		
		onCommand(sender, arguments);
	}
	
	protected abstract void onCommand(CommandSender sender, String[] arguments);
	
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		return new LinkedList<>();
	}
	
	public String getUsage() {
		
		if (isChild()) { return getParent().getParentUsage() + " " + getName(); } else { return "/" + getName(); }
	}
	
	public void sendUsage(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Usage: " + getUsage());
	}
}