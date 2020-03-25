package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class PortalStructure {
	
	private Axis axis;
	private Set<Block> portalBlocks;
	private Set<Block> frameBlocks;
	
	public PortalStructure() {
		this.portalBlocks = new HashSet<>();
		this.frameBlocks = new HashSet<>();
	}
	
	public Location getLocation() {
		return null;
	}
	
	public AxisAlignedRect getPortalRect() {
		return null;
	}
	
	public boolean containsBlock(Block portalBlock) {
		return portalBlocks.contains(portalBlock);
	}
	
	public Axis getAxis() {
		return axis;
	}
}
