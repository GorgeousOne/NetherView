package me.gorgeousone.netherview.blockcache;

import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockCache {
	
	private Block[][][] sourceBlocks;
	private BlockCopy[][][] blockCopies;
	
	private BlockVec min;
	private BlockVec max;
	
	public BlockCache(Block[][][] sourceBlocks, BlockVec offset) {
		
		this.sourceBlocks = sourceBlocks;
		this.min = offset.clone();
		this.max = offset.clone().add(sourceCacheSize());
		
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
	
	public Set<BlockCopy> getCopiesAround(BlockVec blockCorner) {
		
		Set<BlockCopy> blocksAroundCorner = new HashSet<>();
		
		for (BlockVec position : getAllCornerLocs(blockCorner)) {
			
			if (!copiesContain(position))
				continue;
			
			BlockCopy copy = getCopyAt(position);
			
			if(copy != null)
				blocksAroundCorner.add(copy);
		}
		
		return blocksAroundCorner;
	}
	
	private Set<BlockVec> getAllCornerLocs(BlockVec blockCorner) {
		
		Set<BlockVec> locsAroundCorner = new HashSet<>();
		
		int x = blockCorner.getX();
		int y = blockCorner.getY();
		int z = blockCorner.getZ();
		
		locsAroundCorner.add(new BlockVec(x, y, z));
//		locsAroundCorner.add(new BlockVec(x - 1, y, z));
//		locsAroundCorner.add(new BlockVec(x, y, z - 1));
//		locsAroundCorner.add(new BlockVec(x - 1, y, z - 1));
//		locsAroundCorner.add(new BlockVec(x, y - 1, z));
//		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z));
//		locsAroundCorner.add(new BlockVec(x, y - 1, z - 1));
//		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z - 1));
		
		return locsAroundCorner;
	}
	
	private void createBlockCopies() {
		
		BlockVec sourceSize = sourceCacheSize();
		
		blockCopies = new BlockCopy
				[sourceSize.getX()]
				[sourceSize.getY()]
				[sourceSize.getZ()];
		
		for (int x = 0; x < sourceSize.getX(); x++){
			for (int y = 0; y < sourceSize.getY(); y++) {
				for (int z = 0; z < sourceSize.getZ(); z++) {
					
					Block source = sourceBlocks[x][y][z];
					
					if(source != null)
						blockCopies[x][y][z] = new BlockCopy(source);
				}
			}
		}
	}
}
