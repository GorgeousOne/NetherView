package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import org.bukkit.command.CommandSender;

public class EnableDebugCommand extends ArgCommand {
	
	private NetherView main;
	
	public EnableDebugCommand(ParentCommand parent, NetherView main) {
		
		super("enabledebug", null, false, parent);
		addArg(new Argument("flag", ArgType.BOOLEAN));
		
		this.main = main;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		main.setDebugMessagesEnabled(arguments[0].getBoolean());
	}
}