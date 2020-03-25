package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class Line {
	
	private Vector origin;
	private Vector direction;
	
	public Line(Vector start, Vector end) {
		
		this.origin = start.clone();
		this.direction = end.clone().subtract(start);
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
