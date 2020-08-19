package me.gorgeousone.netherview.cmdframework.argument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Argument {
	
	private final String name;
	
	private final ArgType type;
	private final List<String> tabList;
	private ArgValue defValue;
	
	public Argument(String name, ArgType type) {
		this(name, type, new String[]{});
	}
	
	public Argument(String name, ArgType type, String... tabList) {
		
		this.name = name;
		this.type = type;
		
		this.tabList = new ArrayList<>();
		
		if (type == ArgType.BOOLEAN) {
			this.tabList.add("true");
			this.tabList.add("false");
		} else {
			this.tabList.addAll(Arrays.asList(tabList));
		}
	}
	
	public boolean hasDefault() {
		return getDefault() != null;
	}
	
	public ArgValue getDefault() {
		return defValue;
	}
	
	public String getName() {
		return name;
	}
	
	public ArgType getType() {
		return type;
	}
	
	public List<String> getTabList() {
		return tabList;
	}
	
	public Argument setDefaultTo(String value) {
		defValue = new ArgValue(getType(), value);
		return this;
	}
}