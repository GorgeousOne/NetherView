package me.gorgeousone.netherview.wrapping.boundingbox;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntityBoundingBox {
	
	private final Entity entity;
	private Vector lastLocation;
	
	private final List<Vector> vertices;
	
	public EntityBoundingBox(Entity entity, double width, double height) {
		
		this.entity = entity;
		this.lastLocation = entity.getLocation().toVector();
		
		Vector min = entity.getLocation().subtract(
				width / 2,
				height / 2,
				width / 2).toVector();
		
		Vector max = entity.getLocation().add(
				width / 2,
				height / 2,
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
	
	public List<Vector> getVertices() {
		
		Vector currentLocation = entity.getLocation().toVector();
		
		if (!currentLocation.equals(lastLocation)) {
			updateVertices(currentLocation);
		}
		
		return vertices;
	}
	
	private void updateVertices(Vector currentLocation) {
		
		Vector dist = currentLocation.clone().subtract(lastLocation);
		
		for (Vector vertex : vertices) {
			vertex.add(dist);
		}
		
		lastLocation = currentLocation;
	}
}