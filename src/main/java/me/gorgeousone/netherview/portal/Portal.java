package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.wrapper.Axis;
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
	
	private final World world;
	private final AxisAlignedRect portalRect;
	
	private final Set<Block> portalBlocks;
	
	private Portal counterPortal;
	private Transform tpTransform;
	
	private Map.Entry<BlockCache, BlockCache> blockCaches;
	private Map.Entry<ProjectionCache, ProjectionCache> projectionCaches;
	
	private final Cuboid frameShape;
	private final Cuboid innerShape;
	
	private boolean isViewFlipped;
	
	public Portal(World world,
	              AxisAlignedRect portalRect,
	              Cuboid frameShape,
	              Cuboid innerShape,
	              Set<Block> portalBlocks) {
		
		this.world = world;
		this.portalRect = portalRect.clone();
		this.frameShape = frameShape.clone();
		this.innerShape = innerShape.clone();
		this.portalBlocks = portalBlocks;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Location getLocation() {
		return portalRect.getMin().toLocation(world);
	}
	
	public BlockVec getMaxBlockAtFloor() {
		
		BlockVec maxBlock = frameShape.getMax().clone().add(-1, 0, -1);
		maxBlock.setY(frameShape.getMin().getY());
		return maxBlock;
	}
	
	public Cuboid getFrame() {
		return frameShape;
	}
	
	public Cuboid getInner() {
		return innerShape;
	}
	
	public int width() {
		return getAxis() == Axis.X ? frameShape.getWidthX() : frameShape.getWidthZ();
	}
	
	public int height() {
		return frameShape.getHeight();
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
	
	/**
	 * Sets the front and back projection caches for this portal.
	 * @param projectionCaches where the key is referred to as front projection and value as back projection
	 */
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
		return BlockVec.toSimpleString(getLocation());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getLocation());
	}
}