package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;

/**
 * The equivalent to a BlockCache used to store information about all blocks that will be displayed in the animation of a portal.
 * For each of the 2 sides of a portal there will be a separate ProjectionCache.
 */
public class ProjectionCache extends BlockCache {
	
	private final BlockCache sourceCache;
	private final Transform linkTransform;
	private final int cacheLength;
	
	public ProjectionCache(Portal portal,
	                       BlockVec offset,
	                       BlockVec size,
	                       BlockVec facing,
	                       BlockType borderType,
	                       BlockCache sourceCache,
	                       Transform linkTransform) {
		
		super(portal, offset, size, facing, borderType);
		
		this.sourceCache = sourceCache;
		this.linkTransform = linkTransform;
		this.cacheLength = portal.getAxis() == Axis.X ? size.getZ() : size.getX();
	}
	
	public BlockCache getSourceCache() {
		return sourceCache;
	}
	
	public Transform getLinkTransform() {
		return linkTransform;
	}
	
	/**
	 * Returns the length of the projection cache measured from portal to back wall.
	 * The value is important for the length of view frustums.
	 */
	public int getCacheLength() {
		return cacheLength;
	}
}