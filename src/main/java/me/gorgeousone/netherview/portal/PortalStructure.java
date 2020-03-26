package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.threedstuff.PortalRectangle;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class PortalStructure {
	
	private World world;
	private Axis axis;
	private PortalRectangle portalRect;
	private Set<Block> portalBlocks;
	
	public PortalStructure(World world,
	                       PortalRectangle portalRect,
	                       Set<Block> portalBlocks) {
		this.world = world;
		this.axis = portalRect.getAxis();
		this.portalRect = portalRect;
		this.portalBlocks = portalBlocks;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Location getLocation() {
		return portalRect.getSomewhatOfACenter().toLocation(world);
	}
	
	public PortalRectangle getPortalRect() {
		return portalRect;
	}
	
	public boolean containsBlock(Block portalBlock) {
		return portalBlocks.contains(portalBlock);
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public Set<Block> getPortalBlocks() {
		return new HashSet<>(portalBlocks);
	}
}