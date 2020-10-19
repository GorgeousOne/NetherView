package me.gorgeousone.netherview.customportal.commands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class UnlinkPortalCommand extends ArgCommand {
	
	private final ViewHandler viewHandler;
	private final CustomPortalHandler customPortalHandler;
	
	public UnlinkPortalCommand(ParentCommand parent,
	                           ViewHandler viewHandler,
	                           CustomPortalHandler customPortalHandler) {
		
		super("unlink", NetherViewPlugin.CUSTOM_PORTAL_PERM, true, parent);
		this.viewHandler = viewHandler;
		addArg(new Argument("portal", ArgType.STRING));
		
		this.customPortalHandler = customPortalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		String portalName = arguments[0].getString();
		
		Portal portal = customPortalHandler.getPortal(portalName);
		
		if (portal == null) {
			MessageUtils.sendInfo(sender, Message.NO_PORTAL_FOUND_WITH_NAME, portalName);
			return;
		}
		
		if (!portal.isLinked()) {
			return;
		}
		
		viewHandler.removePortal(portal);
		portal.removeLink();
		MessageUtils.sendInfo(sender, Message.UNLINKED_PORTAL, portalName);
	}
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		if (arguments.length <= 1) {
			
			return customPortalHandler.getPortalNames().stream().
					filter(name -> name.startsWith(arguments[arguments.length - 1])).
					collect(Collectors.toList());
		}
		
		return super.getTabList(sender, arguments);
	}
}
