package me.gorgeousone.netherview.cmdframework.command;

import me.gorgeousone.netherview.cmdframework.argument.ArgType;
import me.gorgeousone.netherview.cmdframework.argument.ArgValue;
import me.gorgeousone.netherview.cmdframework.argument.Argument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class ArgCommand extends BasicCommand {
	
	private List<Argument> arguments;
	
	protected ArgCommand(String name, String permission, boolean isPlayerRequired) {
		this(name, permission, isPlayerRequired, null);
	}
	
	protected ArgCommand(String name, String permission, boolean isPlayerRequired, ParentCommand parent) {
		
		super(name, permission, isPlayerRequired, parent);
		this.arguments = new ArrayList<>();
	}
	
	public List<Argument> getArgs() {
		return arguments;
	}
	
	protected void addArg(Argument arg) {
		arguments.add(arg);
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] stringArgs) {
		
		int argsSize = getArgs().size();
		int stringArgsLength = stringArgs.length;
		
		ArgValue[] values = new ArgValue[Math.max(argsSize, stringArgsLength)];
		
		try {
			if (stringArgsLength >= argsSize)
				createMoreValuesThanOwnArgs(values, stringArgs);
			else
				createMoreValuesThanSenderInput(values, stringArgs);
			
		} catch (ArrayIndexOutOfBoundsException e) {
			sendUsage(sender);
			return;
			
		} catch (IllegalArgumentException e) {
			sender.sendMessage(e.getMessage());
			return;
		}
		
		onCommand(sender, values);
		return;
	}
	
	protected abstract void onCommand(CommandSender sender, ArgValue[] arguments);
	
	@Override
	public List<String> getTabList(CommandSender sender, String[] arguments) {
		
		if(isPlayerRequired() && !(sender instanceof Player))
			return null;
		
		if (this.arguments.size() < arguments.length)
			return new LinkedList<>();
		
		return this.arguments.get(arguments.length - 1).getTabList();
	}
	
	@Override
	public String getUsage() {
		
		StringBuilder usage = new StringBuilder(super.getUsage());
		
		for (Argument arg : getArgs()) {
			usage.append(" <");
			usage.append(arg.getName());
			usage.append(">");
		}
		
		return usage.toString();
	}
	
	protected void createMoreValuesThanOwnArgs(ArgValue[] values, String[] stringArgs) {
		
		for (int i = 0; i < values.length; i++) {
			
			values[i] = i < getArgs().size() ?
					new ArgValue(getArgs().get(i).getType(), stringArgs[i]) :
					new ArgValue(ArgType.STRING, stringArgs[i]);
		}
	}
	
	protected void createMoreValuesThanSenderInput(ArgValue[] values, String[] stringArgs) {
		
		for (int i = 0; i < values.length; i++) {
			Argument arg = getArgs().get(i);
			
			if (i < stringArgs.length) {
				values[i] = new ArgValue(getArgs().get(i).getType(), stringArgs[i]);
				continue;
			}
			
			if (arg.hasDefault())
				values[i] = arg.getDefault();
			else
				throw new ArrayIndexOutOfBoundsException();
		}
	}
}