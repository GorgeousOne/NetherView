package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class PortalInfoCommand extends BasicCommand {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	
	public PortalInfoCommand(ParentCommand parent, NetherViewPlugin main, PortalHandler portalHandler) {
		
		super("portalinfo", NetherViewPlugin.INFO_PERM, true, parent);
		
		this.main = main;
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		World world = player.getWorld();
		
		if (!main.canCreatePortalViews(world)) {
			MessageUtils.sendInfo(player, Message.WORLD_NOT_WHITE_LISTED, player.getWorld().getName());
			return;
		}
		
		Portal portal = portalHandler.getClosestPortal(player.getLocation(), false);
		
		if (portal == null) {
			MessageUtils.sendInfo(player, Message.NO_PORTALS_FOUND, player.getWorld().getName());
			return;
		}
		
		String isFlipped = String.valueOf(portal.isViewFlipped());
		String counterPortal = portal.isLinked() ? portal.getCounterPortal().toString() : "-no portal-";
		Set<Portal> connectedPortalsSet = portalHandler.getPortalsLinkedTo(portal);
		StringBuilder connectedPortals;
		
		if (connectedPortalsSet.isEmpty()) {
			connectedPortals = new StringBuilder("-no portals-");
			
		} else {
			connectedPortals = new StringBuilder();
			
			for (Portal connectedPortal : connectedPortalsSet) {
				connectedPortals.append("\\n&7  - &r").append(connectedPortal.toString());
			}
		}
		
		MessageUtils.sendInfo(player, Message.PORTAL_INFO, portal.toString(), isFlipped, counterPortal, connectedPortals.toString());
	}
}