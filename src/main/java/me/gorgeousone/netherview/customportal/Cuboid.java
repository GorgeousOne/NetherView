package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.geometry.BlockVec;

public class Cuboid {
	
	private final BlockVec minVec;
	private final BlockVec maxVec;
	
	public Cuboid(BlockVec pos1, BlockVec pos2) {
		
		this.minVec = BlockVec.getMinimum(pos1, pos2);
		this.maxVec = BlockVec.getMaximum(pos1, pos2);
	}
	
	public BlockVec getMinVec() {
		return minVec;
	}
	
	public BlockVec getMaxVec() {
		return maxVec;
	}
	
	public int getWidthX() {
		return maxVec.getX() - minVec.getX() + 1;
	}
	
	public int getHeight() {
		return maxVec.getY() - minVec.getY() + 1;
	}
	
	public int getWidthZ() {
		return maxVec.getZ() - minVec.getZ() + 1;
	}
}