package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleViewCommand extends BasicCommand {
	
	private final ViewHandler viewHandler;
	
	public ToggleViewCommand(ViewHandler viewHandler) {
		
		super("togglenetherview", NetherViewPlugin.VIEW_PERM, true);
		this.viewHandler = viewHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		
		boolean wantsToSeePortalViews = !viewHandler.hasPortalViewEnabled(player);
		viewHandler.setPortalViewEnabled(player, wantsToSeePortalViews);
		
		player.sendMessage(ChatColor.GRAY + (wantsToSeePortalViews ? "Enabled" : "Disabled") + " portal viewing for you.");
	}
}