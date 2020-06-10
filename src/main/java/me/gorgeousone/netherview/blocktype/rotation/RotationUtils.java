package me.gorgeousone.netherview.blocktype.rotation;

import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotationUtils {
	
	private final static List<BlockFace> ROTATION_FACES = new ArrayList<>(Arrays.asList(
			BlockFace.NORTH_WEST,
			BlockFace.NORTH_NORTH_WEST,
			BlockFace.NORTH,
			BlockFace.NORTH_NORTH_EAST,
			BlockFace.NORTH_EAST,
			
			BlockFace.EAST_NORTH_EAST,
			BlockFace.EAST,
			BlockFace.EAST_SOUTH_EAST,
			
			BlockFace.SOUTH_EAST,
			BlockFace.SOUTH_SOUTH_EAST,
			BlockFace.SOUTH,
			BlockFace.SOUTH_SOUTH_WEST,
			BlockFace.SOUTH_WEST,
			
			BlockFace.WEST_SOUTH_WEST,
			BlockFace.WEST,
			BlockFace.WEST_NORTH_WEST
	));

	/**
	 * Returns the rotated version of a block face
	 *
	 * @param face         face to be rotated
	 * @param quarterTurns count of 90 degree turns that should be performed (0 - 3)
	 */
	public static BlockFace getRotatedFace(BlockFace face, int quarterTurns) {
		
		if (!ROTATION_FACES.contains(face)) {
			return face;
		}
		
		int rotatedFaceIndex = (ROTATION_FACES.indexOf(face) + quarterTurns * 4) % ROTATION_FACES.size();
		return ROTATION_FACES.get(rotatedFaceIndex);
	}
	
}
