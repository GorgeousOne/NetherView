package me.gorgeousone.netherview;

import org.bukkit.block.BlockFace;

public class Utils {

	public static BlockFace[] getAxesFaces() {
		return new BlockFace[] {
				BlockFace.UP,
				BlockFace.DOWN,
				BlockFace.WEST,
				BlockFace.EAST,
				BlockFace.SOUTH,
				BlockFace.NORTH};
	}
}
