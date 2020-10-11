package me.gorgeousone.netherview.geometry;

import org.bukkit.block.Block;

public class Cuboid {
	
	private final BlockVec minBlock;
	private final BlockVec maxBlock;
	
	public Cuboid(BlockVec pos1, BlockVec pos2) {
		
		this.minBlock = BlockVec.getMinimum(pos1, pos2);
		this.maxBlock = BlockVec.getMaximum(pos1, pos2);
	}
	
	public BlockVec getMinBlock() {
		return minBlock.clone();
	}
	
	public BlockVec getMaxBlock() {
		return maxBlock.clone();
	}
	
	public int getWidthX() {
		return maxBlock.getX() - minBlock.getX() + 1;
	}
	
	public int getHeight() {
		return maxBlock.getY() - minBlock.getY() + 1;
	}
	
	public int getWidthZ() {
		return maxBlock.getZ() - minBlock.getZ() + 1;
	}
	
	public boolean contains(BlockVec vec) {
		return vec.getX() >= minBlock.getX() && vec.getX() <= maxBlock.getX() &&
		       vec.getY() >= minBlock.getY() && vec.getY() <= maxBlock.getY() &&
		       vec.getZ() >= minBlock.getZ() && vec.getZ() <= maxBlock.getZ();
	}
	
	public boolean contains(Block block) {
		return block.getX() >= minBlock.getX() && block.getX() <= maxBlock.getX() &&
		       block.getY() >= minBlock.getY() && block.getY() <= maxBlock.getY() &&
		       block.getZ() >= minBlock.getZ() && block.getZ() <= maxBlock.getZ();
	}
}