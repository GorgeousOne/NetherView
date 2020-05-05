package me.gorgeousone.netherview.blockcache;

import org.bukkit.block.Block;

public class CacheCopy {
	
	private BlockCache cache;
	private Transform blockTransform;
	
	private BlockCopy[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	
	public CacheCopy(BlockCache cache, Transform blockTransform) {
		
		this.cache = cache;
		this.blockTransform = blockTransform;
		
		createBlockCopies();
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
	
	public BlockCopy getCopyAt(BlockVec loc) {
		
		if (!contains(loc))
			return null;
		
		return blockCopies
				[loc.getX() - min.getX()]
				[loc.getY() - min.getY()]
				[loc.getZ() - min.getZ()];
	}
	
	public void updateCopy(Block blockCacheBlock) {
		
		BlockCopy newBlockCopy = blockTransform.transformBlockCopy(new BlockCopy(blockCacheBlock));
		BlockVec blockPos = newBlockCopy.getPosition();
		
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = newBlockCopy;
	}
	
	private void createBlockCopies() {
		
		BlockVec cacheMin = cache.getMin();
		BlockVec cacheMax = cache.getMax();
		
		BlockVec corner1 = blockTransform.transformVec(cacheMin.clone());
		BlockVec corner2 = blockTransform.transformVec(cacheMax.clone());
		
		min = BlockVec.getMinimum(corner1, corner2);
		max = BlockVec.getMaximum(corner1, corner2);
		
		int x1 = min.getX();
		int y1 = min.getY();
		int z1 = min.getZ();
		int x2 = max.getX();
		int y2 = max.getY();
		int z2 = max.getZ();
		
		blockCopies = new BlockCopy[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		for (int x = cacheMin.getX(); x < cacheMax.getX(); x++) {
			for (int y = cacheMin.getY(); y < cacheMax.getY(); y++) {
				for (int z = cacheMin.getZ(); z < cacheMax.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockCopy blockCopy = cache.getCopyAt(blockPos);
					
					if (blockCopy == null)
						continue;
					
					BlockCopy transformedBlockCopy = blockTransform.transformBlockCopy(blockCopy.clone());
					BlockVec newBlockPos = transformedBlockCopy.getPosition();
					
					blockCopies[newBlockPos.getX() - x1]
							[newBlockPos.getY() - y1]
							[newBlockPos.getZ() - z1] = transformedBlockCopy;
				}
			}
		}
	}
}
