package me.gorgeousone.netherview;

import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.block.BlockFace;

public class FacingUtils {
	
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
}