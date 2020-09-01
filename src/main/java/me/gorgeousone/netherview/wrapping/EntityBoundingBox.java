package me.gorgeousone.netherview.wrapping;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityBoundingBox {
	
	private final Entity entity;
	private final double sizeX;
	private final double sizeY;
	private final double sizeZ;
	
	public EntityBoundingBox(Entity entity, double sizeX, double sizeY, double sizeZ) {
		
		this.entity = entity;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}
	
	private Vector getMin() {
		
		return entity.getLocation().subtract(
				sizeX / 2,
				sizeY / 2,
				sizeZ / 2).toVector();
	}
	
	private Vector getMax() {
		
		return entity.getLocation().add(
				sizeX / 2,
				sizeY / 2,
				sizeZ / 2).toVector();
	}
}