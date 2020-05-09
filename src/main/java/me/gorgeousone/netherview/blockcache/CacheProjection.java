package me.gorgeousone.netherview.blockcache;

import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class CacheProjection {
	
	private BlockCache sourceCache;
	private Transform blockTransform;
	
	private BlockCopy[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	private World world;
	
	public CacheProjection(BlockCache cache, Transform blockTransform, World world) {
		
		this.sourceCache = cache;
		this.blockTransform = blockTransform;
		this.world = world;
		
		createBlockCopies();
	}
	
	public World getWorld() {
		return world;
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
	
	public Set<BlockCopy> getCopiesAround(BlockVec blockCorner) {
		
		Set<BlockCopy> blocksAroundCorner = new HashSet<>();
		
		for (BlockVec position : getAllCornerLocs(blockCorner)) {
			
			if (!contains(position))
				continue;
			
			BlockCopy copy = getCopyAt(position);
			
			if (copy != null)
				blocksAroundCorner.add(copy.clone());
		}
		
		return blocksAroundCorner;
	}
	
	public BlockCopy updateCopy(BlockCopy sourceCopy) {
		
		BlockCopy newBlockCopy = blockTransform.transformBlockCopy(sourceCopy.clone());
		BlockVec blockPos = newBlockCopy.getPosition();
		
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = newBlockCopy;
		
		return newBlockCopy;
	}
	
	private void createBlockCopies() {
		
		BlockVec sourceMin = sourceCache.getMin();
		BlockVec sourceMax = sourceCache.getMax();
		
		BlockVec corner1 = blockTransform.transformVec(sourceMin.clone());
		BlockVec corner2 = blockTransform.transformVec(sourceMax.clone());
		
		min = BlockVec.getMinimum(corner1, corner2);
		max = BlockVec.getMaximum(corner1, corner2).add(1, 0, 1);
		
		int minX = min.getX();
		int minY = min.getY();
		int minZ = min.getZ();
		
		blockCopies = new BlockCopy
				[max.getX() - minX]
				[max.getY() - minY]
				[max.getZ() - minZ];
		
		for (int x = sourceMin.getX(); x < sourceMax.getX(); x++) {
			for (int y = sourceMin.getY(); y < sourceMax.getY(); y++) {
				for (int z = sourceMin.getZ(); z < sourceMax.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockCopy blockCopy = sourceCache.getCopyAt(blockPos);
					
					if (blockCopy == null)
						continue;
					
					BlockCopy transformedBlockCopy = blockTransform.transformBlockCopy(blockCopy.clone());
					BlockVec newBlockPos = transformedBlockCopy.getPosition();
					
					blockCopies
							[newBlockPos.getX() - minX]
							[newBlockPos.getY() - minY]
							[newBlockPos.getZ() - minZ] = transformedBlockCopy;
				}
			}
		}
	}
	
	private Set<BlockVec> getAllCornerLocs(BlockVec blockCorner) {
		
		Set<BlockVec> locsAroundCorner = new HashSet<>();
		
		int x = blockCorner.getX();
		int y = blockCorner.getY();
		int z = blockCorner.getZ();
		
		locsAroundCorner.add(new BlockVec(x, y, z));
		locsAroundCorner.add(new BlockVec(x, y - 1, z));
		locsAroundCorner.add(new BlockVec(x, y, z - 1));
		locsAroundCorner.add(new BlockVec(x, y - 1, z - 1));
		
		locsAroundCorner.add(new BlockVec(x - 1, y, z));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z));
		locsAroundCorner.add(new BlockVec(x - 1, y, z - 1));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z - 1));
		
		return locsAroundCorner;
	}
}