package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlipPortalCommand extends BasicCommand {
	
	private NetherView main;
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	
	public FlipPortalCommand(ParentCommand parent, NetherView main, PortalHandler portalHandler, ViewHandler viewHandler) {
	
		super("flipportal", NetherView.PORTAL_FLIP_PERM, true, parent);
	
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		World world = player.getWorld();
		
		if (!main.canCreatePortalViews(world)) {
			
			sender.sendMessage(ChatColor.GRAY + "NetherView is not enabled for world '" + world.getName() + "'.");
			sender.sendMessage(ChatColor.GRAY + "You can enable it by adding the world's name to 'worlds-with-portal-viewing' in the config.");
			return;
		}
		
		Portal viewedPortal = viewHandler.getViewedPortal(player);
		
		if (viewedPortal == null) {
			player.sendMessage(ChatColor.GRAY + "Please stand closer to a nether portal that uses NetherView.");
			return;
		}
		
		viewedPortal.flipView();
		portalHandler.loadProjectionCachesOf(viewedPortal);
		
		viewHandler.hideViewSession(player);
		viewHandler.displayClosestPortalTo(player, player.getEyeLocation());
		sender.sendMessage(ChatColor.GRAY + "Flipped view of portal " + viewedPortal.toWhiteString());
	}
}
