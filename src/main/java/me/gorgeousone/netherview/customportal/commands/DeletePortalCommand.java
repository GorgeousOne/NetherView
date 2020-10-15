package me.gorgeousone.netherview.customportal.commands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import org.bukkit.command.CommandSender;

public class DeletePortalCommand extends ArgCommand {
	
	private final PortalHandler portalHandler;
	private final CustomPortalHandler customPortalHandler;
	
	public DeletePortalCommand(ParentCommand parent,
	                           PortalHandler portalHandler,
	                           CustomPortalHandler customPortalHandler) {
		
		super("deleteportal", NetherViewPlugin.CUSTOM_PORTAL_PERM, false, parent);
		addArg(new Argument("portal name", ArgType.STRING));
		
		this.portalHandler = portalHandler;
		this.customPortalHandler = customPortalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		String portalName = arguments[0].getString();
		CustomPortal portal = customPortalHandler.getPortal(portalName);
		
		if (portal == null) {
			MessageUtils.sendInfo(sender, Message.NO_PORTAL_FOUND_WITH_NAME, portalName);
			return;
		}
		
		customPortalHandler.removePortal(portal);
		portalHandler.removePortal(portal);
		MessageUtils.sendInfo(sender, Message.REMOVED_PORTAL, portalName);
	}
}
