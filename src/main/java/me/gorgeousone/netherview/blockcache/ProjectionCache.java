package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	
	public Map<BlockVec, BlockType> getBlockTypesAround(int x, int y, int z) {
		
		Map<BlockVec, BlockType> blocksAroundCorner = new HashMap<>();
		
		for (BlockVec blockPos : getAllCornerLocs(x, y, z)) {
			
			if (!contains(blockPos)) {
				continue;
			}
			
			BlockType blockType = getBlockTypeAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			
			if (blockType != null) {
				blocksAroundCorner.put(blockPos, blockType.clone());
			}
		}
		
		return blocksAroundCorner;
	}
	
	public Map<BlockVec, BlockType> getBlocksAroundPositiveX(int x, int y, int z) {
		
		Map<BlockVec, BlockType> blocksAroundCorner = new HashMap<>();
		
		for (int dy = -1; dy <= 0; dy++) {
			for (int dz = -1; dz <= 0; dz++) {
				
				int newY = y + dy;
				int newZ = z + dz;
				
				BlockType blockType = getBlockTypeAt(x, newY, newZ);
				
				if (blockType != null) {
					blocksAroundCorner.put(new BlockVec(x, newY, newZ), blockType);
				}
			}
		}
		
		return blocksAroundCorner;
	}
	
	public Map<BlockVec, BlockType> getBlocksAroundPositiveZ(int x, int y, int z) {
		
		Map<BlockVec, BlockType> blocksAroundCorner = new HashMap<>();
		
		for (int dy = -1; dy <= 0; dy++) {
			for (int dx = -1; dx <= 0; dx++) {
				
				int newY = y + dy;
				int newX = x + dx;
				
				BlockType blockType = getBlockTypeAt(newX, newY, z);
				
				if (blockType != null) {
					blocksAroundCorner.put(new BlockVec(newX, newY, z), blockType);
				}
			}
		}
		
		return blocksAroundCorner;
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
	
	private Set<BlockVec> getAllCornerLocs(int x, int y, int z) {
		
		Set<BlockVec> locsAroundCorner = new HashSet<>();
		
		for (int dx = -1; dx <= 0; dx++) {
			for (int dy = -1; dy <= 0; dy++) {
				for (int dz = -1; dz <= 0; dz++) {
					
					locsAroundCorner.add(new BlockVec(
							x + dx,
							y + dy,
							z + dz));
				}
			}
		}
		
		return locsAroundCorner;
	}
}