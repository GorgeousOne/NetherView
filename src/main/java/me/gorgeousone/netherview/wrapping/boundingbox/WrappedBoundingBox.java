package me.gorgeousone.netherview.wrapping.boundingbox;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper for the extent of a bounding boxes of an entity throughout different Minecraft versions.
 */
public class WrappedBoundingBox {
	
	private final List<Vector> vertices;
	
	public WrappedBoundingBox(Entity entity, double width, double height) {
		
		Vector min = entity.getLocation().subtract(
				width / 2,
				0,
				width / 2).toVector();
		
		Vector max = entity.getLocation().add(
				width / 2,
				height,
				width / 2).toVector();
		
		vertices = new ArrayList<>(Arrays.asList(
				min,
				new Vector(max.getX(), min.getY(), min.getZ()),
				new Vector(min.getX(), min.getY(), max.getZ()),
				new Vector(max.getX(), min.getY(), max.getZ()),
				new Vector(min.getX(), max.getY(), min.getZ()),
				new Vector(max.getX(), max.getY(), min.getZ()),
				new Vector(min.getX(), max.getY(), max.getZ()),
				max
		));
	}
	
	/**
	 * Returns the 8 vertices of the entity's bounding box
	 */
	public List<Vector> getVertices() {
		return vertices;
	}
}