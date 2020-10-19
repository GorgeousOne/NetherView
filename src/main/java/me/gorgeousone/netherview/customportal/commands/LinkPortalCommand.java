package me.gorgeousone.netherview.customportal.commands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class LinkPortalCommand extends ArgCommand {
	
	private final ViewHandler viewHandler;
	private final PortalHandler portalHandler;
	private final CustomPortalHandler customPortalHandler;
	
	public LinkPortalCommand(ParentCommand parent,
	                         ViewHandler viewHandler, PortalHandler portalHandler,
	                         CustomPortalHandler customPortalHandler) {
		
		super("link", NetherViewPlugin.CUSTOM_PORTAL_PERM, true, parent);
		this.viewHandler = viewHandler;
		addArg(new Argument("from portal", ArgType.STRING));
		addArg(new Argument("to portal", ArgType.STRING));
		
		this.portalHandler = portalHandler;
		this.customPortalHandler = customPortalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		String portalName1 = arguments[0].getString();
		String portalName2 = arguments[1].getString();
		
		Portal portal1 = customPortalHandler.getPortal(portalName1);
		Portal portal2 = customPortalHandler.getPortal(portalName2);
		
		if (portal1 == null) {
			MessageUtils.sendInfo(sender, Message.NO_PORTAL_FOUND_WITH_NAME, portalName1);
			return;
		}
		
		if (portal2 == null) {
			MessageUtils.sendInfo(sender, Message.NO_PORTAL_FOUND_WITH_NAME, portalName2);
			return;
		}
		
		Player player = (Player) sender;
		
		try {
			viewHandler.removePortal(portal1);
			portalHandler.linkPortalTo(portal1, portal2, player);
			viewHandler.displayClosestPortalTo(player, player.getEyeLocation());
			MessageUtils.sendInfo(sender, Message.LINKED_PORTAL, portalName1, portalName2);
			
		} catch (MessageException e) {
			MessageUtils.sendInfo(sender, e.getPlayerMessage(), e.getPlaceholderValues());
		}
	}
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		if (arguments.length <= 2) {
			
			return customPortalHandler.getPortalNames().stream().
					filter(name -> name.startsWith(arguments[arguments.length - 1])).
					collect(Collectors.toList());
		}
		
		return super.getTabList(sender, arguments);
	}
}
