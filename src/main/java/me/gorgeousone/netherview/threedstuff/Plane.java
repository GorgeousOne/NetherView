package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class Plane {
	
	private Vector origin;
	private Vector normal;
	
	public Plane(Vector origin, Vector normal) {
		this.origin = origin.clone();
		this.normal = normal.clone().normalize();
		
		if (normal.equals(new Vector(0, 0, 0)))
			throw new IllegalArgumentException("normal cannot be 0");
	}
	
	public Vector getOrigin() {
		return origin.clone();
	}
	
	public Vector getNormal() {
		return normal.clone();
	}
	
	public boolean contains(Vector point) {
		
		if (point == null)
			return false;
		
		Vector subtract = getOrigin().subtract(point);
		return Math.abs(getNormal().dot(subtract)) < 0.0001;
	}
	
	public Vector getIntersection(DefinedLine l) {
		
		double d = getOrigin().subtract(l.getOrigin()).dot(getNormal()) / l.getDirection().dot(getNormal());
		Vector intersection = l.getPoint(d);
		
		return contains(intersection) ? intersection : null;
	}
}
