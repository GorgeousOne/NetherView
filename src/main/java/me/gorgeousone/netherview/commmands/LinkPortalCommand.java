package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.MessageException;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.command.CommandSender;

public class LinkPortalCommand extends ArgCommand {
	
	private final PortalHandler portalHandler;
	
	public LinkPortalCommand(ParentCommand parent,
	                         PortalHandler portalHandler) {
		
		super("link", NetherViewPlugin.CUSTOM_PORTAL_PERM, false, parent);
		addArg(new Argument("from portal", ArgType.INTEGER));
		addArg(new Argument("to portal", ArgType.INTEGER));
		
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
	
		String portalName1 = arguments[0].getString();
		String portalName2 = arguments[1].getString();
		
		Portal portal1 = portalHandler.getPortalByHash(arguments[0].getInt());
		Portal portal2 = portalHandler.getPortalByHash(arguments[1].getInt());
		
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
			MessageUtils.sendInfo(sender, Message.PORTALS_LINKED, portalName1, portalName2);
			
		}catch (MessageException e) {
			MessageUtils.sendInfo(sender, e.getPlayerMessage(), e.getPlaceholderValues());
		}
	}
}
