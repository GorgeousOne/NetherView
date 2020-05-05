package me.gorgeousone.netherview.blockcache;

import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockCache {
	
	private BlockCopy[][][] blockCopies;
	
	private BlockVec min;
	private BlockVec max;
	
	public BlockCache(BlockVec offset, BlockCopy[][][] blockCopies) {
		
		this.blockCopies = blockCopies;
		this.min = offset.clone();
		this.max = offset.clone().add(sourceCacheSize());
	}
	
	private BlockVec sourceCacheSize() {
		return new BlockVec(blockCopies.length, blockCopies[0].length, blockCopies[0][0].length);
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
	
	public void updateCopy(Block block) {
		
		blockCopies
				[block.getX() - min.getX()]
				[block.getY() - min.getY()]
				[block.getZ() - min.getZ()].setData(block.getBlockData());
	}
	
	public BlockCopy getCopyAt(BlockVec loc) {
		
		if (!contains(loc))
			return null;
		
		BlockCopy copy = blockCopies
				[loc.getX() - min.getX()]
				[loc.getY() - min.getY()]
				[loc.getZ() - min.getZ()];
		
		return copy == null ? null : copy.clone();
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
