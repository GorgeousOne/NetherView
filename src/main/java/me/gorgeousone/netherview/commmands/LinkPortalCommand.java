package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.message.MessageUtils;
import org.bukkit.command.CommandSender;

public class LinkPortalCommand extends ArgCommand {
	
	private final PortalHandler portalHandler;
	private final CustomPortalHandler customPortalHandler;
	public LinkPortalCommand(ParentCommand parent,
	                         PortalHandler portalHandler,
	                         CustomPortalHandler customPortalHandler) {
		
		super("link", NetherViewPlugin.CUSTOM_PORTAL_PERM, false, parent);
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
		
		try {
			portalHandler.linkPortalTo(portal1, portal2, sender);
			MessageUtils.sendInfo(sender, Message.LINKED_PORTALS, portalName1, portalName2);
			
		} catch (MessageException e) {
			MessageUtils.sendInfo(sender, e.getPlayerMessage(), e.getPlaceholderValues());
		}
	}
}
