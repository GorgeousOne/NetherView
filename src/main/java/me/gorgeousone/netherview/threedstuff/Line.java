package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class Line {
	
	private Vector origin;
	private Vector direction;
	
	public Line(Vector point1, Vector point2) {
		
		this.origin = point1.clone();
		this.direction = point2.clone().subtract(point1);
	}
	
	public Vector getOrigin() {
		return origin.clone();
	}
	
	public Vector getDirection() {
		return direction.clone();
	}
	
	public Vector getPoint(double d) {
		return getOrigin().add(getDirection().multiply(d));
	}
}
