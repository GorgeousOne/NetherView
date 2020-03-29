package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockCache {
	
	private Block[][][] sourceBlocks;
	private BlockCopy[][][] blockCopies;
	
	private BlockVec min;
	private BlockVec max;
	private Transform blockTransform;
	
	public BlockCache(Block[][][] sourceBlocks, BlockVec offset, Transform blockTransform) {
		
		this.sourceBlocks = sourceBlocks;
		this.blockTransform = blockTransform;
		
		createTransformedBounds(offset);
		createBlockCopies();
	}
	
	private BlockVec sourceCacheSize() {
		return new BlockVec(sourceBlocks.length, sourceBlocks[0].length, sourceBlocks[0][0].length);
	}
	
	public BlockVec getCopyMin() {
		return min.clone();
	}
	
	public BlockVec getCopyMax() {
		return max.clone();
	}
	
	public BlockCopy getCopyAt(BlockVec loc) {
		return blockCopies
				[loc.getX() - min.getX()]
				[loc.getY() - min.getY()]
				[loc.getZ() - min.getZ()];
	}
	
	public boolean copiesContain(BlockVec loc) {
		return loc.getX() >= min.getX() && loc.getX() < max.getX() &&
		       loc.getY() >= min.getY() && loc.getY() < max.getY() &&
		       loc.getZ() >= min.getZ() && loc.getZ() < max.getZ();
	}
	
	public Set<BlockCopy> getCopiesAround(int x, int y, int z) {
		
		Set<BlockCopy> blocksAroundCorner = new HashSet<>();
		
		for (BlockVec position : getAllCornerLocs(x, y, z)) {
			if (!copiesContain(position))
				continue;
			
			BlockCopy copy = getCopyAt(position);
			
			if(copy != null)
				blocksAroundCorner.add(copy);
		}
		
		return blocksAroundCorner;
	}
	
	private Set<BlockVec> getAllCornerLocs(int x, int y, int z) {
		
		Set<BlockVec> locsAroundCorner = new HashSet<>();
		
		locsAroundCorner.add(new BlockVec(x, y, z));
		locsAroundCorner.add(new BlockVec(x - 1, y, z));
		locsAroundCorner.add(new BlockVec(x, y, z - 1));
		locsAroundCorner.add(new BlockVec(x - 1, y, z - 1));
		locsAroundCorner.add(new BlockVec(x, y - 1, z));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z));
		locsAroundCorner.add(new BlockVec(x, y - 1, z - 1));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z - 1));
		
		return locsAroundCorner;
	}
	
	private void createTransformedBounds(BlockVec offset) {
		
		BlockVec sourceSize = sourceCacheSize();
		
		BlockVec transformedMin = blockTransform.getTransformed(offset);
		BlockVec transformedMax = transformedMin.clone().add(blockTransform.rotate(sourceSize));
		
		min = BlockVec.getMinimum(transformedMin, transformedMax);
		max = BlockVec.getMaximum(transformedMin, transformedMax);
	}
	
	private void createBlockCopies() {
		
		BlockVec sourceSize = sourceCacheSize();
		BlockVec copySize = blockTransform.rotate(sourceSize.clone());
		
		blockCopies = new BlockCopy
				[copySize.getX()]
				[copySize.getY()]
				[copySize.getZ()];
		
		for (int x = 0; x < sourceSize.getX(); x++){
			for (int y = 0; y < sourceSize.getY(); y++) {
				for (int z = 0; z < sourceSize.getZ(); z++) {
				
					Block block = sourceBlocks[x][y][z];
					
					if(block == null)
						continue;
					
					BlockVec copyPos = blockTransform.getTransformed(new BlockVec(block));
					BlockCopy blockCopy = new BlockCopy(copyPos, block.getBlockData());
					BlockVec copyIndex = blockTransform.rotate(new BlockVec(x, y, z));
					
					blockCopies[copyIndex.getX()]
							[copyIndex.getY()]
							[copyIndex.getZ()] = blockCopy;
				}
			}
		}
	}
}
