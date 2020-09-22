package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
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
		sender.sendMessage(NetherViewPlugin.CHAT_PREFIX + " Reloaded config settings.");
	}
}