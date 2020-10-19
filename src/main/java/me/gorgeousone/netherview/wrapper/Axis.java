package me.gorgeousone.netherview.wrapper;

import org.bukkit.util.Vector;

/**
 * A replacement for the Axis enum of bukkit which did not exist before 1.13.
 * It is used to describe the orientation of portals which can be along the x-axis or the z-axis.
 */
public enum Axis {
	
	X(new Vector(0, 0, 1), new Vector(1, 0, 0)),
	Z(new Vector(1, 0, 0), new Vector(0, 0, 1));
	
	private final Vector normal;
	private final Vector crossNormal;
	
	Axis(Vector normal, Vector crossNormal) {
		this.normal = normal;
		this.crossNormal = crossNormal;
	}
	
	/**
	 * Returns the normal vector for the plane aligned to this axis.
	 */
	public Vector getNormal() {
		return normal.clone();
	}
	
	/**
	 * Returns a vector that is inside the plane aligned to this axis.
	 * It like the cross product of the plane normal with the vector (0|1|0) but it's never negative.
	 */
	public Vector getCrossNormal() {
		return crossNormal.clone();
	}
}
