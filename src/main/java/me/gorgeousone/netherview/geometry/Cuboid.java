package me.gorgeousone.netherview.geometry;

import org.bukkit.block.Block;

public class Cuboid {
	
	private final BlockVec min;
	private final BlockVec max;
	
	public Cuboid(BlockVec pos1, BlockVec pos2) {
		
		this.min = BlockVec.getMinimum(pos1, pos2);
		this.max = BlockVec.getMaximum(pos1, pos2);
	}
	
	public BlockVec getMin() {
		return min.clone();
	}
	
	public BlockVec getMax() {
		return max.clone();
	}
	
	public int getWidthX() {
		return max.getX() - min.getX();
	}
	
	public int getHeight() {
		return max.getY() - min.getY();
	}
	
	public int getWidthZ() {
		return max.getZ() - min.getZ();
	}
	
	public boolean contains(BlockVec vec) {
		return vec.getX() >= min.getX() && vec.getX() < max.getX() &&
		       vec.getY() >= min.getY() && vec.getY() < max.getY() &&
		       vec.getZ() >= min.getZ() && vec.getZ() < max.getZ();
	}
	
	public boolean contains(Block block) {
		return block.getX() >= min.getX() && block.getX() < max.getX() &&
		       block.getY() >= min.getY() && block.getY() < max.getY() &&
		       block.getZ() >= min.getZ() && block.getZ() < max.getZ();
	}
	
	public Cuboid translateMin(int dx, int dy, int dz) {
		min.add(dx, dy, dz);
		return this;
	}
	
	public Cuboid translateMin(BlockVec vec) {
		min.add(vec);
		return this;
	}
	
	public Cuboid translateMax(int dx, int dy, int dz) {
		max.add(dx, dy, dz);
		return this;
	}
	
	public Cuboid translateMax(BlockVec vec) {
		max.add(vec);
		return this;
	}
	
	public boolean intersects(Cuboid otherBox) {
		return intersectsX(otherBox) && intersectsY(otherBox) && intersectsZ(otherBox) ||
		       otherBox.intersectsX(this) && otherBox.intersectsY(this) && otherBox.intersectsZ(this);
	}
	
	public boolean intersectsX(Cuboid otherBox) {
		return containsX(otherBox.min.getX()) || containsX(otherBox.max.getX() - 1) || otherBox.min.getX() < min.getX() && otherBox.max.getX() > max.getX();
	}
	
	public boolean intersectsY(Cuboid otherBox) {
		return containsY(otherBox.min.getY()) || containsY(otherBox.max.getY() - 1) || otherBox.min.getY() < min.getY() && otherBox.max.getY() > max.getY();
	}
	
	public boolean intersectsZ(Cuboid otherBox) {
		return containsZ(otherBox.min.getZ()) || containsZ(otherBox.max.getZ() - 1) || otherBox.min.getZ() < min.getZ() && otherBox.max.getZ() > max.getZ();
	}
	
	public boolean containsX(double x) {
		return x >= min.getX() && x < max.getX();
	}
	
	public boolean containsY(double y) {
		return y >= min.getY() && y < max.getY();
	}
	
	public boolean containsZ(double z) {
		return z >= min.getZ() && z <= max.getZ();
	}
	
	@Override
	public Cuboid clone() {
		return new Cuboid(min, max);
	}
}