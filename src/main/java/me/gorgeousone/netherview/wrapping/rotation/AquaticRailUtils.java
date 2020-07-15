package me.gorgeousone.netherview.wrapping.rotation;

import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AquaticRailUtils {
	
	private final static List<Rail.Shape> STRAIGHT_RAILS = new ArrayList<>(Arrays.asList(
			Rail.Shape.NORTH_SOUTH,
			Rail.Shape.EAST_WEST
	));
	
	private final static List<Rail.Shape> CURVED_RAILS = new ArrayList<>(Arrays.asList(
			Rail.Shape.NORTH_WEST,
			Rail.Shape.NORTH_EAST,
			Rail.Shape.SOUTH_EAST,
			Rail.Shape.SOUTH_WEST
	));
	
	private final static List<Rail.Shape> ASCENDING_RAILS = new ArrayList<>(Arrays.asList(
			Rail.Shape.ASCENDING_NORTH,
			Rail.Shape.ASCENDING_EAST,
			Rail.Shape.ASCENDING_SOUTH,
			Rail.Shape.ASCENDING_WEST
	));
	
	private AquaticRailUtils() {}
	
	public static Rail.Shape getRotatedRail(Rail.Shape railShape, int quarterTurns) {
		
		if (STRAIGHT_RAILS.contains(railShape)) {
			
			if (quarterTurns % 2 == 0) {
				return railShape;
			}
			
			return railShape == Rail.Shape.NORTH_SOUTH ? Rail.Shape.EAST_WEST : Rail.Shape.NORTH_SOUTH;
			
		} else if (CURVED_RAILS.contains(railShape)) {
			
			int rotatedShapeIndex = (CURVED_RAILS.indexOf(railShape) + quarterTurns) % CURVED_RAILS.size();
			return CURVED_RAILS.get(rotatedShapeIndex);
			
		} else {
			
			int rotatedShapeIndex = (ASCENDING_RAILS.indexOf(railShape) + quarterTurns) % ASCENDING_RAILS.size();
			return ASCENDING_RAILS.get(rotatedShapeIndex);
		}
	}
}
