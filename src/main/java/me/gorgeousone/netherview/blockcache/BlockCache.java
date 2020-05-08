package me.gorgeousone.netherview.blockcache;

import org.bukkit.block.Block;

public class BlockCache {
	
	private BlockCopy[][][] blockCopies;
	
	private BlockVec min;
	private BlockVec max;
	
	public BlockCache(BlockVec offset, BlockCopy[][][] blockCopies) {
		
		this.blockCopies = blockCopies;
		this.min = offset.clone();
		this.max = offset.clone().add(sourceCacheSize());
		
		System.out.println(min.toString() + " s " + max.toString());
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
}
