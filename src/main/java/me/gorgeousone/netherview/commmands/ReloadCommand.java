package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends BasicCommand {
	
	private final NetherViewPlugin main;
	
	public ReloadCommand(ParentCommand parent, NetherViewPlugin main) {
		
		super("reload", NetherViewPlugin.CONFIG_PERM, false, parent);
		addAlias("rl");
		
		this.main = main;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		main.reload();
		sender.sendMessage(ChatColor.DARK_RED + "[" + ChatColor.DARK_PURPLE + "NV" + ChatColor.DARK_RED + "]" + ChatColor.LIGHT_PURPLE + " Reloaded config settings.");
	}
}