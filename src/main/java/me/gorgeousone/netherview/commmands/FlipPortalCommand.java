package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlipPortalCommand extends BasicCommand {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	
	public FlipPortalCommand(ParentCommand parent,
	                         NetherViewPlugin main,
	                         PortalHandler portalHandler,
	                         ViewHandler viewHandler) {
		
		super("flipportal", NetherViewPlugin.PORTAL_FLIP_PERM, true, parent);
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		World world = player.getWorld();
		
		if (!main.canCreatePortalViews(world)) {
			MessageUtils.sendInfo(player, Message.WORLD_NOT_WHITE_LISTED, player.getWorld().getName());
			return;
		}
		
		Portal viewedPortal = viewHandler.getViewSession(player).getViewedPortal();
		
		if (viewedPortal == null) {
			MessageUtils.sendInfo(player, Message.NO_PORTAL_FOUND_NEARBY);
			return;
		}
		
		viewedPortal.flipView();
		portalHandler.loadProjectionCachesOf(viewedPortal);
		
		viewHandler.hidePortalProjection(player);
		viewHandler.displayClosestPortalTo(player, player.getEyeLocation());
		MessageUtils.sendInfo(player, Message.FLIPPED_PORTAL, viewedPortal.toString());
	}
}
