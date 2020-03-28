package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

/**
 * An euclidean line where only points between the start and the end of it can be accessed.
 */
public class DefinedLine {
	
	private Vector origin;
	private Vector direction;
	
	public DefinedLine(Vector start, Vector end) {
		
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
		return d < 0 || d > 1 ? null : getOrigin().add(getDirection().multiply(d));
	}
}
