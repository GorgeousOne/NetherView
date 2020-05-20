package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PortalInfoCommand extends BasicCommand {
	
	private PortalHandler portalHandler;
	
	public PortalInfoCommand(ParentCommand parent, PortalHandler portalHandler) {
		
		super("listportals", null, true, parent);
		
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		Portal nearestPortal = portalHandler.getNearestPortal(player.getLocation(), false);
		
		//TODO send player infos about portal
	}
}
