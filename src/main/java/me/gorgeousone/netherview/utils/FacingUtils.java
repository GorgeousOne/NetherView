package me.gorgeousone.netherview.utils;

import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.wrapping.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class FacingUtils {
	
	private FacingUtils() {}
	
	public static BlockFace[] getAxesFaces() {
		return new BlockFace[]{
				BlockFace.UP,
				BlockFace.DOWN,
				BlockFace.WEST,
				BlockFace.EAST,
				BlockFace.SOUTH,
				BlockFace.NORTH};
	}
	
	public static BlockVec[] getAxesBlockVecs() {
		return new BlockVec[]{
				new BlockVec(1, 0, 0),
				new BlockVec(0, 1, 0),
				new BlockVec(0, 0, 1),
				new BlockVec(-1, 0, 0),
				new BlockVec(0, -1, 0),
				new BlockVec(0, 0, -1)};
	}
	
	public static Axis getAxis(Block portalBlock) {
		return portalBlock.getData() == 2 ? Axis.Z : Axis.X;
	}
}