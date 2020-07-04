package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * The equivalent to a BlockCache used to store information about all blocks that will be displayed in the animation of a portal.
 * For each of the 2 sides of a portal there will be a separate ProjectionCache.
 */
public class ProjectionCache {
	
	private Portal portal;
	private BlockType[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	
	private Transform blockTransform;
	private int cacheLength;
	private Vector facing;
	private BlockType borderType;
	
	public ProjectionCache(Portal portal,
	                  BlockVec offset,
	                  BlockVec size,
	                  Vector facing,
	                  BlockType borderType) {
		
		this.portal = portal;
		this.blockCopies = new BlockType[size.getX()][size.getY()][size.getZ()];
		this.min = offset.clone();
		this.max = offset.clone().add(size());
		
		this.facing = facing;
		this.borderType = borderType;
		
		if (portal.getAxis() == Axis.X) {
			cacheLength = blockCopies[0][0].length;
		} else {
			cacheLength = blockCopies.length;
		}
	}
	
	private BlockVec size() {
		return new BlockVec(blockCopies.length, blockCopies[0].length, blockCopies[0][0].length);
	}
	
	public void setBlockTransform(Transform blockTransform) {
		this.blockTransform = blockTransform;
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public World getWorld() {
		return portal.getWorld();
	}
	
	public Transform getTransform() {
		return blockTransform;
	}
	
	public BlockVec getMin() {
		return min.clone();
	}
	
	public BlockVec getMax() {
		return max.clone();
	}
	
	/**
	 * Returns the length of the projection cache measured from portal to back wall.
	 * The value is important for the length of viewing frustums.
	 */
	public int getCacheLength() {
		return cacheLength;
	}
	
	public boolean contains(BlockVec loc) {
		return loc.getX() >= min.getX() && loc.getX() < max.getX() &&
		       loc.getY() >= min.getY() && loc.getY() < max.getY() &&
		       loc.getZ() >= min.getZ() && loc.getZ() < max.getZ();
	}
	
	public boolean contains(int x, int y, int z) {
		return x >= min.getX() && x < max.getX() &&
		       y >= min.getY() && y < max.getY() &&
		       z >= min.getZ() && z < max.getZ();
	}
	
	public BlockType getBlockTypeAt(int x, int y, int z) {
		
		if (!contains(x, y, z)) {
			return null;
		}
		
		return blockCopies
				[x - min.getX()]
				[y - min.getY()]
				[z - min.getZ()];
	}
	
	public void setBlockTypeAt(BlockVec blockPos, BlockType blockType) {
		setBlockTypeAt(
				blockPos.getX(),
				blockPos.getY(),
				blockPos.getZ(),
				blockType);
	}
	
	public void setBlockTypeAt(int x, int y, int z, BlockType blockType) {
		blockCopies
				[x - min.getX()]
				[y - min.getY()]
				[z - min.getZ()] = blockType;
	}
}