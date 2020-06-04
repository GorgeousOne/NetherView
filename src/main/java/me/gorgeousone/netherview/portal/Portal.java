package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Portal {
	
	private World world;
	private AxisAlignedRect portalRect;
	
	private Set<Block> portalBlocks;
	private Set<Block> frameBlocks;
	
	//bounds containing all portal blocks including frame
	private BlockVec min;
	private BlockVec max;
	
	private Portal counterPortal;
	
	private Map.Entry<BlockCache, BlockCache> blockCaches;
	private Map.Entry<ProjectionCache, ProjectionCache> projectionCaches;
	
	private boolean exists;
	
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
		
		this.exists = true;
	}
	
	public void remove() {
		this.exists = false;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Location getLocation() {
		return portalRect.getMin().toLocation(world);
	}
	
	public AxisAlignedRect getPortalRect() {
		return portalRect.clone();
	}
	
	public Axis getAxis() {
		return portalRect.getAxis();
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
	
	public void setLinkedTo(Portal counterPortal) {
		this.counterPortal = counterPortal;
	}
	
	public void removeLink() {
		this.counterPortal = null;
		removeProjectionCaches();
	}
	
	public boolean isLinked() {
		
		if (counterPortal == null)
			return false;
		
		if (!counterPortal.exists()) {
			removeLink();
			return false;
		}
		
		return true;
	}
	
	private boolean exists() {
		return exists;
	}
	
	public void setBlockCaches(Map.Entry<BlockCache, BlockCache> blockCaches) {
		this.blockCaches = blockCaches;
	}
	
	public void removeBlockCaches() {
		blockCaches = null;
	}
	
	public boolean blockCachesAreLoaded() {
		return blockCaches != null;
	}
	
	public BlockCache getFrontCache() {
		return blockCaches.getKey();
	}
	
	public BlockCache getBackCache() {
		return blockCaches.getValue();
	}
	
	public void setProjectionCaches(Map.Entry<ProjectionCache, ProjectionCache> projectionCaches) {
		this.projectionCaches = projectionCaches;
	}
	
	public void removeProjectionCaches() {
		this.projectionCaches = null;
	}
	
	public boolean projectionsAreLoaded() {
		return projectionCaches != null;
	}
	
	public ProjectionCache getFrontProjection() {
		return projectionCaches.getKey();
	}
	
	public ProjectionCache getBackProjection() {
		return projectionCaches.getValue();
	}
	
	@Override
	public String toString() {
		return '[' + world.getName() + ", " + new BlockVec(getLocation()).toString() + ']';
	}
	
	public String toWhiteString() {
		return ChatColor.RESET + toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getLocation());
	}
}