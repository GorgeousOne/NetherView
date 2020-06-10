package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.World;

/**
 * The equivalent to a BlockCache used to store information about all blocks that will be displayed in the animation of a portal.
 * For each of the 2 sides of a portal there will be a separate ProjectionCache.
 */
public class ProjectionCache {
	
	private Portal portal;
	private Transform blockTransform;
	
	private BlockType[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	
	private int cacheLength;
	
	public ProjectionCache(Portal projectedPortal, BlockCache sourceCache, Transform blockTransform) {
		
		this.portal = projectedPortal;
		this.blockTransform = blockTransform;
		
		createBlockCopies(sourceCache);
		
		if (portal.getAxis() == Axis.X) {
			cacheLength = blockCopies[0][0].length;
		} else {
			cacheLength = blockCopies.length;
		}
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
	
	public void setBlockTypeAt(BlockVec blockPos, BlockType newBlockData) {
		
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = newBlockData;
	}
	
	private void createBlockCopies(BlockCache sourceCache) {
		
		BlockVec sourceMin = sourceCache.getMin();
		BlockVec sourceMax = sourceCache.getMax();
		
		BlockVec corner1 = blockTransform.transformVec(sourceMin.clone());
		BlockVec corner2 = blockTransform.transformVec(sourceMax.clone());
		
		min = BlockVec.getMinimum(corner1, corner2);
		max = BlockVec.getMaximum(corner1, corner2).add(1, 0, 1);
		
		int minX = min.getX();
		int minY = min.getY();
		int minZ = min.getZ();
		
		blockCopies = new BlockType
				[max.getX() - minX]
				[max.getY() - minY]
				[max.getZ() - minZ];
		
		for (int x = sourceMin.getX(); x < sourceMax.getX(); x++) {
			for (int y = sourceMin.getY(); y < sourceMax.getY(); y++) {
				for (int z = sourceMin.getZ(); z < sourceMax.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockType blockType = sourceCache.getBlockTypeAt(blockPos);
					
					if (blockType == null) {
						continue;
					}
					
					BlockType rotatedBlockType = blockType.clone().rotate(blockTransform.getQuarterTurns());
					BlockVec newBlockPos = blockTransform.transformVec(blockPos);
					
					blockCopies
							[newBlockPos.getX() - minX]
							[newBlockPos.getY() - minY]
							[newBlockPos.getZ() - minZ] = rotatedBlockType;
				}
			}
		}
	}
}