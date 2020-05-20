package me.gorgeousone.netherview.cmdframework.argument;

import org.bukkit.ChatColor;

public class ArgValue {
	
	private static final String argumentTypeException = ChatColor.RED + "'%value%' is not a %type%.";
	
	private int intVal;
	private double decimalVal;
	private String stringVal;
	private boolean booleanVal;
	
	public ArgValue(String stringValue) {
		this(ArgType.STRING, stringValue);
	}
	
	public ArgValue(ArgType type, String value) {
		setValue(value, type);
	}
	
	public String getString() {
		return stringVal;
	}
	
	public int getInt() {
		return intVal;
	}
	
	public double getDouble() {
		return decimalVal;
	}
	
	public boolean getBoolean() {
		return booleanVal;
	}
	
	protected void setValue(String value, ArgType type) {
		
		try {
			switch (type) {
				
				case INTEGER:
					intVal = Integer.parseInt(value);
				
				case DECIMAL:
					decimalVal = Double.parseDouble(value);
				
				case STRING:
					stringVal = value;
					break;
				
				case BOOLEAN:
					booleanVal = Boolean.parseBoolean(value);
					break;
			}
			
		} catch (Exception ex) {
			throw new IllegalArgumentException(argumentTypeException.replace("%value%", value).replace("%type%", type.simpleName()));
		}
	}
}
