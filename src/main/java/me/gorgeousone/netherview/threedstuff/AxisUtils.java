package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class AxisUtils {
	
	public static Vector getAxisPlaneNormal(Axis axis) {
		
		switch (axis) {
			case X:
				return new Vector(0, 0, 1);
			case Y:
				return new Vector(0, 1, 0);
			default:
				return new Vector(1, 0, 0);
		}
	}
	
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
