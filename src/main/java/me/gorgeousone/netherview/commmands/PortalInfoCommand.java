package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.ChatColor;
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
			
			sender.sendMessage(ChatColor.GRAY + "NetherView is not enabled for world '" + world.getName() + "'.");
			sender.sendMessage(ChatColor.GRAY + "You can enable it by adding the world's name to 'worlds-with-portal-viewing' in the config.");
			return;
		}
		
		Portal portal = portalHandler.getClosestPortal(player.getLocation(), false);
		
		if (portal == null) {
			
			sender.sendMessage(ChatColor.GRAY + "There are no nether portals listed for world '" + player.getWorld().getName() + "'.");
			return;
		}
		
		player.sendMessage(ChatColor.GRAY + "Info about portal at " + portal.toWhiteString() + ":");
		player.sendMessage(ChatColor.GRAY + "  is flipped: " + ChatColor.RESET + portal.isViewFlipped());
		
		if (portal.isLinked()) {
			
			player.sendMessage(ChatColor.GRAY + "  is linked to:");
			player.sendMessage(ChatColor.GRAY + "  - " + portal.getCounterPortal().toWhiteString());
			
		} else {
			player.sendMessage(ChatColor.GRAY + "  is linked to: -no portal-");
		}
		
		Set<Portal> connectedPortals = portalHandler.getPortalsLinkedTo(portal);
		
		if (connectedPortals.isEmpty()) {
			player.sendMessage(ChatColor.GRAY + "  portals linked to portal: -none-");
			
		} else {
			
			player.sendMessage(ChatColor.GRAY + "  portals linked to portal: ");
			
			for (Portal counterPortal : connectedPortals) {
				player.sendMessage(ChatColor.GRAY + "  - " + counterPortal.toWhiteString());
			}
		}
	}
}