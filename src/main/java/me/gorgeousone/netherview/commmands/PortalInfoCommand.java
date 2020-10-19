package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class PortalInfoCommand extends BasicCommand {
	
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	
	public PortalInfoCommand(ParentCommand parent, ConfigSettings configSettings, PortalHandler portalHandler) {
		
		super("portalinfo", NetherViewPlugin.INFO_PERM, true, parent);
		
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		World world = player.getWorld();
		
		if (!configSettings.canCreatePortalViews(world)) {
			MessageUtils.sendInfo(player, Message.WORLD_NOT_WHITE_LISTED, player.getWorld().getName());
			return;
		}
		
		Portal portal = portalHandler.getClosestPortal(player.getLocation(), false);
		
		if (portal == null) {
			MessageUtils.sendInfo(player, Message.NO_PORTALS_FOUND, player.getWorld().getName());
			return;
		}
		
		StringBuilder infoText = new StringBuilder();

//		 &7  is flipped: &r%is-flipped%
//          &7  is linked to: &r%counter-portal%
//         &7  portals linked to portal: &r%linked-portals%
		
		if (portal instanceof CustomPortal) {
			infoText.append("\\n" + ChatColor.GRAY + "name: " + ChatColor.RESET + ((CustomPortal) portal).getName());
		}
		
		infoText.append("\\n" + ChatColor.GRAY + "is flipped: " + ChatColor.RESET + portal.isViewFlipped());
		infoText.append("\\n" + ChatColor.GRAY + "is linked: " + ChatColor.RESET + (portal.isLinked() ? portal.getCounterPortal().toString() : "-no portal-"));
		infoText.append("\\n" + ChatColor.GRAY + "is linked: " + ChatColor.RESET);
		
		Set<Portal> connectedPortals = portalHandler.getPortalsLinkedTo(portal);
		
		if (connectedPortals.isEmpty()) {
			infoText.append("-no portal-");
			
		} else {
			
			for (Portal connectedPortal : connectedPortals) {
				infoText.append("\\n" + ChatColor.GRAY + " - " + ChatColor.RESET + connectedPortal.toString());
			}
		}
		
		MessageUtils.sendInfo(player, Message.PORTAL_INFO, portal.toString(), infoText.toString());
	}
}