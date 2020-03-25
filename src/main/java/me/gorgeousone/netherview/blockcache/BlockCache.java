package me.gorgeousone.netherview.blockcache;

import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BlockCache {
	
	private BlockState[][][] blocks;
	private Vector min;
	private Vector max;
	
	public BlockCache(BlockState[][][] blocks, Vector offset) {
		this.blocks = blocks;
		this.min = offset.clone();
		this.max = offset.clone().add(new Vector(blocks.length, blocks[0].length, blocks[0][0].length));
	}
	
	public Vector getMin() {
		return min.clone();
	}
	
	public Vector getMax() {
		return max.clone();
	}
	
	public BlockState getBlockAt(Vector loc) {
		return blocks[loc.getBlockX()][loc.getBlockY()][loc.getBlockZ()];
	}
	
	public boolean containsLoc(Vector loc) {
		return loc.getBlockX() >= min.getX() && loc.getBlockX() < max.getX() &&
		       loc.getBlockY() >= min.getY() && loc.getBlockY() < max.getY() &&
		       loc.getBlockZ() >= min.getZ() && loc.getBlockZ() < max.getZ();
	}
	
	public Set<BlockState> getBlocksAtCorner(int x, int y, int z) {
		
		Set<BlockState> blocksAtCorner = new HashSet<>();
		
		for (Vector loc : getAllCornerLocs(x, y, z)) {
			if(containsLoc(loc))
				blocksAtCorner.add(getBlockAt(loc));
		}
		
		return blocksAtCorner;
	}
	
	private Set<Vector> getAllCornerLocs(int x, int y, int z) {
		
		Set<Vector> locsAtCorner = new HashSet<>();
		
		locsAtCorner.add(new Vector(x, y, z));
		locsAtCorner.add(new Vector(x - 1, y, z));
		locsAtCorner.add(new Vector(x, y, z - 1));
		locsAtCorner.add(new Vector(x - 1, y, z - 1));
		locsAtCorner.add(new Vector(x, y - 1, z));
		locsAtCorner.add(new Vector(x - 1, y - 1, z));
		locsAtCorner.add(new Vector(x, y - 1, z - 1));
		locsAtCorner.add(new Vector(x - 1, y - 1, z - 1));
		
		return locsAtCorner;
	}
}
