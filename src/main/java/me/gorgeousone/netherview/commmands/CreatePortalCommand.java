package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.CustomPortal;
import me.gorgeousone.netherview.customportal.CustomPortalHandler;
import me.gorgeousone.netherview.customportal.PlayerCuboidSelection;
import me.gorgeousone.netherview.customportal.PlayerSelectionHandler;
import me.gorgeousone.netherview.customportal.PortalCreator;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class CreatePortalCommand extends ArgCommand {
	
	private final static String genericNamePlaceHolder = "auto_inc";
	
	private final PlayerSelectionHandler selectionHandler;
	private final PortalHandler portalHandler;
	private final CustomPortalHandler customPortalHandler;
	
	public CreatePortalCommand(ParentCommand parent,
	                           PlayerSelectionHandler selectionHandler,
	                           PortalHandler portalHandler,
	                           CustomPortalHandler customPortalHandler) {
		
		super("createportal", NetherViewPlugin.CUSTOM_PORTAL_PERM, true, parent);
		addArg(new Argument("portal name", ArgType.STRING).setDefaultTo(genericNamePlaceHolder));
		
		this.selectionHandler = selectionHandler;
		this.portalHandler = portalHandler;
		this.customPortalHandler = customPortalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		Player player = (Player) sender;
		
		if (!selectionHandler.hasCuboidSelection(player)) {
			player.sendMessage("pls select");
			return;
		}
		
		PlayerCuboidSelection selection = selectionHandler.getSelection(player);
		
		if (!selection.bothPositionsAreSet()) {
			player.sendMessage("pls select 2 points");
			return;
		}
		
		CustomPortal portal;
		
		try {
			portal = PortalCreator.createPortal(player.getWorld(), selection.getCuboid());
			
		} catch (MessageException e) {
			MessageUtils.sendInfo(player, e.getPlayerMessage(), e.getPlaceholderValues());
			return;
		}
		
		if (portalHandler.portalIntersectsOtherPortals(portal)) {
			MessageUtils.sendInfo(player, Message.PORTALS_INTERSECT);
			return;
		}
		
		String portalName = arguments[0].getString().toLowerCase(Locale.ENGLISH);
		
		if (portalName.equals(genericNamePlaceHolder)) {
			portalName = customPortalHandler.createGenericPortalName();
			
		} else if (!customPortalHandler.isValidName(portalName)) {
			MessageUtils.sendInfo(player, Message.PORTAL_NAME_NOT_VALID);
			return;
		
		}else if (!customPortalHandler.isUniqueName(portalName)) {
			MessageUtils.sendInfo(player, Message.PORTAL_NAME_NOT_UNIQUE, portalName);
			return;
		}
		
		portal.setName(portalName);
		portalHandler.addPortal(portal);
		customPortalHandler.addPortal(portal);
		MessageUtils.sendInfo(player, Message.CREATED_PORTAL, portalName, portal.width() + "x" + portal.height());
	}
}