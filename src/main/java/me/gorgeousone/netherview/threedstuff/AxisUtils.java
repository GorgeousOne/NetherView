package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class AxisUtils {
	
	/**
	 * Returns the normal vector for the plane of a portal with the passed axis.
	 */
	public static Vector getAxisPlaneNormal(Axis axis) {
		
		switch (axis) {
			case X:
				return new Vector(0, 0, 1);
			case Y:
				return new Vector(0, 1, 0);
			case Z:
				return new Vector(1, 0, 0);
			default:
				return null;
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
