package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreatePortalCommand extends ArgCommand {
	
	private final PlayerSelectionHandler selectionHandler;
	
	protected CreatePortalCommand(ParentCommand parent, PlayerSelectionHandler selectionHandler) {
		
		super("createportal", NetherViewPlugin.CUSTOM_PORTAL_PERM, true, parent);
		
		this.selectionHandler = selectionHandler;
		addArg(new Argument("name", ArgType.STRING).setDefaultTo("AUTO_INC"));
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
		
		Cuboid selectedCuboid = new Cuboid(selection.getPos1(), selection.getPos2());
		
		if (selectedCuboid.getWidthX() > 1 && selectedCuboid.getWidthZ() > 1) {
			player.sendMessage("only one of x and z can be bigger than 1");
			return;
		}
		
		String portalName = arguments[0].getString();
		
		
	}
}