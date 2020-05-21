package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListPortalsCommand extends ArgCommand {
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	public ListPortalsCommand(ParentCommand parent, NetherView main, PortalHandler portalHandler) {
		
		super("listportals", NetherView.INFO_PERM, false, parent);
		addArg(new Argument("world", ArgType.STRING));
		
		this.main = main;
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
		
		String worldName = arguments[0].getString();
		World world = Bukkit.getWorld(worldName);
		
		if (world == null) {
			sender.sendMessage(ChatColor.GRAY + "No world found with name '" + worldName + "'.");
			return;
		}
		
		if (!portalHandler.hasPortals(world)) {
			sender.sendMessage(ChatColor.GRAY + "No portals listed for world '" + worldName + "'.");
			return;
		}
		
		Set<Portal> portalSet = portalHandler.getPortals(world);
		sender.sendMessage(ChatColor.GRAY + "" + portalSet.size() + " portal(s) listed for world '" + worldName + "':");
		
		for (Portal portal : portalHandler.getPortals(world)) {
			sender.sendMessage(ChatColor.GRAY + "- " + portal.toWhiteString());
		}
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
