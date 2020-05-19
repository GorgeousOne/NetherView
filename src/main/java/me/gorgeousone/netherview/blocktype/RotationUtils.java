package me.gorgeousone.netherview.blocktype;

import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotationUtils {
	
	private static List<BlockFace> rotationFaces = new ArrayList<>(Arrays.asList(
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
			BlockFace.WEST_NORTH_WEST));
	
	public static boolean isRotatableFace(BlockFace face) {
		return rotationFaces.contains(face);
	}
	
	public static BlockFace getRotatedFace(BlockFace face, int quarterTurns) {
		
		if (!rotationFaces.contains(face))
			return null;
		
		int positiveTurns = (quarterTurns % 4 + 4);
		int rotatedFaceIndex = (rotationFaces.indexOf(face) + positiveTurns * 4) % rotationFaces.size();
		return rotationFaces.get(rotatedFaceIndex);
	}
}
