package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class EnableDebugCommand extends ArgCommand {
	
	private NetherView main;
	
	public EnableDebugCommand(ParentCommand parent, NetherView main) {
		
		super("debugmessages", NetherView.INFO_PERM, false, parent);
		addArg(new Argument("true/false", ArgType.BOOLEAN));
		
		this.main = main;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		boolean newState = arguments[0].getBoolean();
		boolean stateChanged = main.setDebugMessagesEnabled(newState);
		
		if (stateChanged) {
			sender.sendMessage(ChatColor.DARK_RED + "[" + ChatColor.DARK_PURPLE + "NV" + ChatColor.DARK_RED + "]"
			                   + ChatColor.LIGHT_PURPLE + (newState ? " Enabled" : " Disabled") + " debug messages.");
		}
	}
}