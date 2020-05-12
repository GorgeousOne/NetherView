package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Portal {
	
	private World world;
	private AxisAlignedRect portalRect;
	
	private Set<Block> portalBlocks;
	private Set<Block> frameBlocks;
	
	private BlockVec min;
	private BlockVec max;
	
	private Portal counterPortal;
	
	private Map.Entry<BlockCache, BlockCache> blockCaches;
	private Map.Entry<ProjectionCache, ProjectionCache> projectionCaches;
	
	public Portal(World world,
	              AxisAlignedRect portalRect,
	              Set<Block> portalBlocks,
	              Set<Block> frameBlocks,
	              BlockVec min,
	              BlockVec max) {
		
		this.world = world;
		this.portalRect = portalRect;
		
		this.portalBlocks = portalBlocks;
		this.frameBlocks = frameBlocks;
		
		this.min = min;
		this.max = max;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Location getLocation() {
		return portalRect.getMin().toLocation(world);
	}
	
	public Axis getAxis() {
		return portalRect.getAxis();
	}
	
	public AxisAlignedRect getPortalRect() {
		return portalRect.clone();
	}
	
	public Set<Block> getPortalBlocks() {
		return new HashSet<>(portalBlocks);
	}
	
	public Set<Block> getFrameBlocks() {
		return frameBlocks;
	}
	
	public boolean contains(BlockVec loc) {
		return loc.getX() >= min.getX() && loc.getX() < max.getX() &&
		       loc.getY() >= min.getY() && loc.getY() < max.getY() &&
		       loc.getZ() >= min.getZ() && loc.getZ() < max.getZ();
	}
	
	public boolean equalsInSize(Portal other) {
		
		AxisAlignedRect otherRect = other.getPortalRect();
		
		return portalRect.width() == otherRect.width() &&
		       portalRect.height() == otherRect.height();
	}
	
	public Portal getCounterPortal() {
		return counterPortal;
	}
	
	public void setLinkedTo(Portal counterPortal, Map.Entry<ProjectionCache, ProjectionCache> projectionCaches) {
		this.counterPortal = counterPortal;
		this.projectionCaches = projectionCaches;
	}
	
	public void unlink() {
		this.counterPortal = null;
		this.projectionCaches = null;
	}
	
	public boolean isLinked() {
		return counterPortal != null;
	}
	
	public void setBlockCaches(Map.Entry<BlockCache, BlockCache> blockCaches) {
		this.blockCaches = blockCaches;
	}
	
	public BlockCache getFrontCache() {
		return blockCaches.getKey();
	}
	
	public BlockCache getBackCache() {
		return blockCaches.getValue();
	}
	
	public ProjectionCache getFrontProjection() {
		return projectionCaches.getKey();
	}
	
	public ProjectionCache getBackProjection() {
		return projectionCaches.getValue();
	}
}