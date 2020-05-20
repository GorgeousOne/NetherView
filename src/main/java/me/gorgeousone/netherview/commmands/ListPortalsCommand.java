package me.gorgeousone.netherview.commmands;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import me.gorgeousone.netherview.cmdframework.command.ArgCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.handlers.PortalHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListPortalsCommand extends ArgCommand {
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	public ListPortalsCommand(ParentCommand parent, NetherView main, PortalHandler portalHandler) {
		
		super("listportals", null, false, parent);
		addArg(new Argument("world", ArgType.STRING));
		
		this.main = main;
		this.portalHandler = portalHandler;
	}
	
	@Override
	protected void onCommand(CommandSender sender, ArgValue[] arguments) {
	
	}
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		List<String> worldNames = new ArrayList<>();
		
		for (UUID worldID : main.getWorldsWithPortals()) {
			
			World world = Bukkit.getWorld(worldID);

			if (world != null)
				worldNames.add(world.getName());
		}
		
		return worldNames;
	}
}
