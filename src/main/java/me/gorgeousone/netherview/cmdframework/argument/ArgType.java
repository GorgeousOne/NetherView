package me.gorgeousone.netherview.cmdframework.argument;

public enum ArgType {
	
	INTEGER("integer"),
	DECIMAL("number"),
	STRING("string"),
	BOOLEAN("boolean");
	
	private final String simpleName;
	
	ArgType(String simpleName) {
		this.simpleName = simpleName;
	}
	
	public String simpleName() {
		return simpleName;
	}
}