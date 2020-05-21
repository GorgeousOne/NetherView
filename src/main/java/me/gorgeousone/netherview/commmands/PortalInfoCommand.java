package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class PortalInfoCommand extends BasicCommand {
	
	private PortalHandler portalHandler;
	
	public PortalInfoCommand(ParentCommand parent, PortalHandler portalHandler) {
		
		super("portalinfo", null, true, parent);
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		Portal portal = portalHandler.getNearestPortal(player.getLocation(), false);
		
		if(portal == null) {
			sender.sendMessage(ChatColor.GRAY + "No portals listed for world '" + player.getWorld().getName() + "'.");
			return;
		}
		
		player.sendMessage(ChatColor.GRAY + "Info about portal at " + portal.toWhiteString() + ":");
		
		if (portal.isLinked()) {
			player.sendMessage(ChatColor.GRAY + "  is linked to:");
			player.sendMessage(ChatColor.GRAY + "  - " + portal.getCounterPortal().toWhiteString());
			
		}else {
			player.sendMessage(ChatColor.GRAY + "  is linked: false");
		}
		
		Set<Portal> connectedPortals = portalHandler.getLinkedPortals(portal);
		
		if (connectedPortals.isEmpty()) {
			player.sendMessage(ChatColor.GRAY + "  portals linked to portal: -none-");
		
		}else {
			
			for(Portal counterPortal : connectedPortals) {
				player.sendMessage(ChatColor.GRAY + "  portals linked to portal: ");
				player.sendMessage(ChatColor.GRAY + "  - " + counterPortal.toWhiteString());
			}
		}
	}
}