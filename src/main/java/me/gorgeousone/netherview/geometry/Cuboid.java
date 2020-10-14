package me.gorgeousone.netherview.geometry;

import org.bukkit.block.Block;

public class Cuboid {
	
	private final BlockVec minimum;
	private final BlockVec maximum;
	
	public Cuboid(BlockVec pos1, BlockVec pos2) {
		
		this.minimum = BlockVec.getMinimum(pos1, pos2);
		this.maximum = BlockVec.getMaximum(pos1, pos2);
	}
	
	public BlockVec getMin() {
		return minimum.clone();
	}
	
	public BlockVec getMax() {
		return maximum.clone();
	}
	
	public int getWidthX() {
		return maximum.getX() - minimum.getX();
	}
	
	public int getHeight() {
		return maximum.getY() - minimum.getY();
	}
	
	public int getWidthZ() {
		return maximum.getZ() - minimum.getZ();
	}
	
	public boolean contains(BlockVec vec) {
		return vec.getX() >= minimum.getX() && vec.getX() < maximum.getX() &&
		       vec.getY() >= minimum.getY() && vec.getY() < maximum.getY() &&
		       vec.getZ() >= minimum.getZ() && vec.getZ() < maximum.getZ();
	}
	
	public boolean contains(Block block) {
		return block.getX() >= minimum.getX() && block.getX() < maximum.getX() &&
		       block.getY() >= minimum.getY() && block.getY() < maximum.getY() &&
		       block.getZ() >= minimum.getZ() && block.getZ() < maximum.getZ();
	}
	
	public Cuboid translateMin(int dx, int dy, int dz) {
		minimum.add(dx, dy, dz);
		return this;
	}
	
	public Cuboid translateMin(BlockVec vec) {
		minimum.add(vec);
		return this;
	}
	
	public Cuboid translateMax(int dx, int dy, int dz) {
		maximum.add(dx, dy, dz);
		return this;
	}
	
	public Cuboid translateMax(BlockVec vec) {
		maximum.add(vec);
		return this;
	}
	
	@Override
	public Cuboid clone() {
		return new Cuboid(minimum, maximum);
	}
}