package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.customportal.PlayerCuboidSelection;
import me.gorgeousone.netherview.customportal.PlayerSelectionHandler;
import me.gorgeousone.netherview.customportal.PortalCreator;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.MessageException;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreatePortalCommand extends ArgCommand {
	
	private final PlayerSelectionHandler selectionHandler;
	private final PortalHandler portalHandler;
	
	public CreatePortalCommand(ParentCommand parent,
	                              PlayerSelectionHandler selectionHandler,
	                              PortalHandler portalHandler) {
		
		super("createportal", NetherViewPlugin.CUSTOM_PORTAL_PERM, true, parent);
		addArg(new Argument("name", ArgType.STRING).setDefaultTo("AUTO_INC"));
		
		this.selectionHandler = selectionHandler;
		this.portalHandler = portalHandler;
		
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
		
		try {
			Portal portal = PortalCreator.createPortal(player.getWorld(), selection.getCuboid());
			portalHandler.addPortal(portal);
			player.sendMessage("created portal " + selection.getPos1() + " " + selection.getPos2());
			
		}catch (MessageException e) {
			MessageUtils.sendInfo(player, e.getPlayerMessage(), e.getPlaceholderValues());
		}
	}
}