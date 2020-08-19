package me.gorgeousone.netherview.wrapping;

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
	
	public Vector getNormal() {
		return normal.clone();
	}
	
	
	public Vector getCrossNormal() {
		return crossNormal.clone();
	}
}
