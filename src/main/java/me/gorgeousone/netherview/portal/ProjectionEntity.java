package me.gorgeousone.netherview.portal;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Random;

public class ProjectionEntity {
	
	private final static Random RANDOM = new Random();
	
	private final Entity entity;
	private final int fakeId;
	private Location lastLoc;
	
	public ProjectionEntity(Entity entity) {
	
		this.entity = entity;
		//chances to match an existing id are like 10,000/2,000,000,000 which is 0,0005%. hope that's enough
		this.fakeId = RANDOM.nextInt();
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public int getFakeId() {
		return fakeId;
	}
	
	public Location getLastLoc() {
		return lastLoc;
	}
	
	public void updateLastLoc() {
		lastLoc = entity.getLocation();
	}
}
