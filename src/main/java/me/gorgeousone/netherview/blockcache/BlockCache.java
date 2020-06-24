package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * One big array of BlockTypes used to store information about all blocks in a cuboid area around a portal.
 * For each of the 2 sides of a portal there is a separate BlockCache.
 */
public class BlockCache {
	
	private Portal portal;
	private BlockType[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	
	private Vector facing;
	private BlockType borderType;
	
	public BlockCache(Portal portal,
	                  BlockVec offset,
	                  BlockType[][][] blockCopies,
	                  Vector facing,
	                  BlockType borderType) {
		
		this.portal = portal;
		this.blockCopies = blockCopies;
		this.min = offset.clone();
		this.max = offset.clone().add(sourceCacheSize());
		
		this.facing = facing;
		this.borderType = borderType;
	}
	
	private BlockVec sourceCacheSize() {
		return new BlockVec(blockCopies.length, blockCopies[0].length, blockCopies[0][0].length);
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public World getWorld() {
		return portal.getWorld();
	}
	
	public BlockVec getMin() {
		return min.clone();
	}
	
	public BlockVec getMax() {
		return max.clone();
	}
	
	public boolean contains(BlockVec loc) {
		return loc.getX() >= min.getX() && loc.getX() < max.getX() &&
		       loc.getY() >= min.getY() && loc.getY() < max.getY() &&
		       loc.getZ() >= min.getZ() && loc.getZ() < max.getZ();
	}
	
	/**
	 * Returns true if the block is at any position bordering the cuboid except the side facing the portal.
	 */
	public boolean isBorder(BlockVec loc) {
		
		if (loc.getY() == min.getY() || loc.getY() == max.getY() - 1) {
			return true;
		}
		
		int x = loc.getX();
		int z = loc.getZ();
		
		int minX = min.getX();
		int minZ = min.getZ();
		int maxX = max.getX() - 1;
		int maxZ = max.getZ() - 1;
		
		if (facing.getZ() != 0) {
			if (x == minX || x == maxX) {
				return true;
			}
		} else if (z == minZ || z == maxZ) {
			return true;
		}
		
		if (facing.getX() == 1) {
			return x == maxX;
		}
		if (facing.getX() == -1) {
			return x == minX;
		}
		if (facing.getZ() == 1) {
			return z == maxZ;
		} else {
			return z == minZ;
		}
	}
	
	public BlockType getBorderBlockType() {
		return borderType;
	}
	
	public BlockType getBlockTypeAt(BlockVec blockPos) {
		
		if (!contains(blockPos)) {
			return null;
		}
		
		return blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()];
	}
	
	public void setBlockTypeAt(BlockVec blockPos, BlockType blockType) {
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = blockType;
	}
	
	public void removeBlockDataAt(BlockVec blockPos) {
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = null;
	}
	
	/**
	 * Returns true if the block copy at the given position is listed as visible (when it's not null)
	 */
	public boolean isBlockListedVisible(BlockVec blockPos) {
		return getBlockTypeAt(blockPos) != null;
	}
	
	/**
	 * Returns true if the block copy at the given position is visible by checking the surrounding block copies for transparent ones
	 */
	public boolean isBlockNowVisible(BlockVec blockPos) {
		
		for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
			
			BlockVec touchingBlockPos = blockPos.clone().add(facing);
			
			if (!contains(touchingBlockPos)) {
				continue;
			}
			
			BlockType touchingBlock = getBlockTypeAt(touchingBlockPos);
			
			if (touchingBlock != null && !touchingBlock.isOccluding()) {
				return true;
			}
		}
		
		//TODO check if block is directly in front of the portal.
		return false;
	}
}
