package me.gorgeousone.netherview.utils;

import com.comphenix.protocol.wrappers.EnumWrappers;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FacingUtils {
	
	private FacingUtils() {}
	
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
	
	public static EnumWrappers.Direction getBlockFaceToDirection(BlockFace face) {
		return EnumWrappers.Direction.valueOf(face.name());
	}
}