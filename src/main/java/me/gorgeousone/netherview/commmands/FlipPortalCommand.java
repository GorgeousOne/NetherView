package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlipPortalCommand extends BasicCommand {
	
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	
	public FlipPortalCommand(ParentCommand parent,
	                         ConfigSettings configSettings,
	                         PortalHandler portalHandler,
	                         ViewHandler viewHandler) {
		
		super("flipportal", NetherViewPlugin.PORTAL_FLIP_PERM, true, parent);
		
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		World world = player.getWorld();
		
		if (!configSettings.canCreatePortalViews(world)) {
			MessageUtils.sendInfo(player, Message.WORLD_NOT_WHITE_LISTED, player.getWorld().getName());
			return;
		}
		
		if (!viewHandler.hasViewSession(player)) {
			MessageUtils.sendInfo(player, Message.NO_PORTAL_FOUND_NEARBY);
			return;
		}
		
		Portal viewedPortal = viewHandler.getViewSession(player).getViewedPortal();

		viewHandler.removePortal(viewedPortal);
		viewedPortal.flipView();
		portalHandler.loadProjectionCachesOf(viewedPortal);
		viewHandler.displayClosestPortalTo(player, player.getEyeLocation());
		
		MessageUtils.sendInfo(player, Message.FLIPPED_PORTAL, viewedPortal.toString());
	}
}
