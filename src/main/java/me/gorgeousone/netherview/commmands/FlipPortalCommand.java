package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlipPortalCommand extends BasicCommand {
	
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	
	public FlipPortalCommand(ParentCommand parent, NetherView main, PortalHandler portalHandler, ViewHandler viewHandler) {
	
		super("flipportal", NetherView.PORTAL_FLIP_PERM, true, parent);
	
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		Portal viewedPortal = viewHandler.getViewedPortal(player);
		
		if (viewedPortal == null) {
			player.sendMessage(ChatColor.GRAY + "Please get closer to a nether portal that uses NetherView.");
			return;
		}
		
		viewedPortal.flipProjections();
		portalHandler.loadProjectionCachesOf(viewedPortal);
		
		viewHandler.hideViewSession(player);
		viewHandler.displayNearestPortalTo(player, player.getEyeLocation());
		sender.sendMessage(ChatColor.GRAY + "Flipped view of portal " + viewedPortal.toWhiteString());
	}
}
