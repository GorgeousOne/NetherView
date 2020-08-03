package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.wrapping.Axis;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class containing information about a located portal structure in a world.
 */
public class Portal {
	
	private World world;
	private AxisAlignedRect portalRect;
	
	private Set<Block> portalBlocks;
	private Set<Block> frameBlocks;
	
	//bounds containing the whole portal structure
	private BlockVec min;
	private BlockVec max;
	
	private Portal counterPortal;
	private Transform tpTransform;
	
	private Map.Entry<BlockCache, BlockCache> blockCaches;
	private Map.Entry<ProjectionCache, ProjectionCache> projectionCaches;
	
	private boolean isViewFlipped;
	
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
	
	public BlockVec getMinBlock() {
		return min.clone();
	}
	
	public BlockVec getMaxBlockAtFloor() {
		
		BlockVec maxBlock = max.clone();
		maxBlock.add(-1, 0, -1);
		maxBlock.setY(min.getY());
		
		return maxBlock;
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
	
	/**
	 * Returns true if the given BlockVec is inside portal structure including the frame.
	 */
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
	
	public void setTpTransform(Transform tpTransform) {
		this.tpTransform = tpTransform;
	}
	
	public Transform getTpTransform() {
		return tpTransform;
	}
	
	public void setLinkedTo(Portal counterPortal) {
		this.counterPortal = counterPortal;
	}
	
	public void removeLink() {
		this.counterPortal = null;
		this.tpTransform = null;
		removeProjectionCaches();
	}
	
	public boolean isLinked() {
		return counterPortal != null;
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
	
	/**
	 * Returns true if the the 2 projections of the portal have been switched with each other for aesthetic reasons.
	 */
	public boolean isViewFlipped() {
		return isViewFlipped;
	}
	
	/**
	 * Sets whether the 2 projections of the portal are switched with each other or not.
	 * The {@link ProjectionCache}s have to be set again to realize this change.
	 */
	public void setViewFlipped(boolean isViewFlipped) {
		this.isViewFlipped = isViewFlipped;
	}
	
	public void flipView() {
		isViewFlipped = !isViewFlipped;
	}
	
	@Override
	public String toString() {
		return '[' + world.getName() + "," + new BlockVec(getLocation()).toString() + ']';
	}
	
	public String toWhiteString() {
		return ChatColor.RESET + toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getLocation());
	}
}