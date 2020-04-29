package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.NoSuchElementException;

public class AxisUtils {
	
	/**
	 * Returns the normal vector for the plane of a portal with the passed axis.
	 */
	public static Vector getAxisPlaneNormal(Axis axis) {
		
		switch (axis) {
			case X:
				return new Vector(0, 0, 1);
			case Z:
				return new Vector(1, 0, 0);
			default:
				throw new NoSuchElementException("Portals can only face in x or z direction.");
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
