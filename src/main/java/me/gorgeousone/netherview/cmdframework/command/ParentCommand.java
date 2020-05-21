package me.gorgeousone.netherview.cmdframework.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParentCommand extends BasicCommand {
	
	private List<BasicCommand> children;
	private String childrenType;
	
	public ParentCommand(String name, String permission, boolean isPlayerRequired, String childrenType) {
		this(name, permission, isPlayerRequired, childrenType, null);
	}
	
	public ParentCommand(String name,
	                     String permission,
	                     boolean isPlayerRequired,
	                     String childrenType,
	                     ParentCommand parent) {
		super(name, permission, isPlayerRequired, parent);
		
		this.childrenType = "<" + childrenType + ">";
		this.children = new ArrayList<>();
	}
	
	public void addChild(BasicCommand child) {
		children.add(child);
	}
	
	@Override
	public void onCommand(CommandSender sender, String[] arguments) {
		
		if (arguments.length == 0) {
			sendUsage(sender);
			return;
		}
		
		for (BasicCommand child : getChildren()) {
			
			if (child.matches(arguments[0])) {
				child.execute(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
				return;
			}
		}
		
		sendUsage(sender);
	}
	
	public List<BasicCommand> getChildren() {
		return children;
	}
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		List<String> tabList = new ArrayList<>();
		
		//create a tab list of the children commands
		if (arguments.length == 1) {
			
			for (BasicCommand child : getChildren()) {
				
				if (child.isPlayerRequired() && !(sender instanceof Player)) {
					continue;
				}
				
				String subPermission = child.getPermission();
				
				if (subPermission == null || sender.hasPermission(subPermission)) {
					tabList.add(child.getName());
				}
			}
			
			return tabList;
		}
		
		//forwards the task of creating a tab list to a child commands
		for (BasicCommand child : getChildren()) {
			
			if (!child.matches(arguments[0]))
				continue;
			
			if (child.isPlayerRequired() && !(sender instanceof Player)) {
				continue;
			}
			
			String subPermission = getPermission();
			
			if (subPermission == null || sender.hasPermission(subPermission)) {
				return child.getTabList(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
			}
		}
		
		return tabList;
	}
	
	@Override
	public String getUsage() {
		return super.getUsage() + " " + childrenType;
	}
	
	public String getParentUsage() {
		return super.getUsage();
	}
}