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
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListPortalsCommand extends ArgCommand {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	
	public ListPortalsCommand(ParentCommand parent, NetherViewPlugin main, PortalHandler portalHandler) {
		
		super("listportals", NetherViewPlugin.INFO_PERM, false, parent);
		addArg(new Argument("world", ArgType.STRING));
		
		this.main = main;
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		String worldName = arguments[0].getString();
		World world = Bukkit.getWorld(worldName);
		
		if (world == null) {
			MessageUtils.sendInfo(sender, Message.NO_WORLD_FOUND, worldName);
			return;
		}
		
		if (!main.canCreatePortalViews(world)) {
			MessageUtils.sendInfo(sender, Message.WORLD_NOT_WHITE_LISTED, worldName);
			return;
		}
		
		if (!portalHandler.hasPortals(world)) {
			MessageUtils.sendInfo(sender, Message.NO_PORTALS_FOUND, worldName);
			return;
		}
		
		Set<Portal> portalSet = portalHandler.getPortals(world);
		StringBuilder portals = new StringBuilder();
		
		for (Portal portal : portalSet) {
			portals.append("\\n").append(ChatColor.GRAY + "- ").append(ChatColor.RESET).append(portal.toString());
		}
		
		MessageUtils.sendInfo(sender, Message.WORLD_INFO, String.valueOf(portalSet.size()), worldName, portals.toString());
	}
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		List<String> worldNames = new ArrayList<>();
		
		for (World world : Bukkit.getWorlds()) {
			
			if (world != null) {
				worldNames.add(world.getName());
			}
		}
		
		return worldNames;
	}
}
