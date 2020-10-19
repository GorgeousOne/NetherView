package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TogglePortalViewCommand extends BasicCommand {
	
	private final ViewHandler viewHandler;
	
	public TogglePortalViewCommand(ViewHandler viewHandler) {
		
		super("toggleportalview", NetherViewPlugin.VIEW_PERM, true);
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		
		boolean wantsToSeePortalViews = !viewHandler.hasPortalViewEnabled(player);
		viewHandler.setPortalViewEnabled(player, wantsToSeePortalViews);
		
		MessageUtils.sendInfo(player, wantsToSeePortalViews ? Message.PORTAL_VIEWING_ON : Message.PORTAL_VIEWING_OFF);
	}
}