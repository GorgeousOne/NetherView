package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugDisplayPortal extends BasicCommand {
	
	private ViewHandler viewHandler;
	
	public DebugDisplayPortal(ViewHandler viewHandler) {
		super("d", null, true);
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		viewHandler.displayNearestPortalTo(player, player.getEyeLocation());
	}
}
