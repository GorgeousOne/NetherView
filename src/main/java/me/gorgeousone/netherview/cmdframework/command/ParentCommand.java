package me.gorgeousone.netherview.cmdframework.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ParentCommand extends BasicCommand {
	
	private List<BasicCommand> children;
	private String childrenType;
	
	public ParentCommand(String name, String permission, boolean isPlayerRequired, String childrenType) {
		this(name, permission, isPlayerRequired, childrenType, null);
	}
	
	public ParentCommand(String name, String permission, boolean isPlayerRequired, String childrenType, ParentCommand parent) {
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
		
		if (arguments.length == 1) {
			List<String> tabList = new LinkedList<>();
			
			for (BasicCommand child : getChildren()) {
				
				if(child.isPlayerRequired() && !(sender instanceof Player))
					continue;

				tabList.add(child.getName());
			}
			return tabList;
		}
		
		for (BasicCommand child : getChildren()) {
			
			if (child.matches(arguments[0]))
				return child.getTabList(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
		}
		
		return new LinkedList<>();
	}
	
	@Override
	public String getUsage() {
		return super.getUsage() + " " + childrenType;
	}
	
	public String getParentUsage() {
		return super.getUsage();
	}
}