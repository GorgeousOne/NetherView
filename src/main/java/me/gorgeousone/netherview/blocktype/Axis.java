package me.gorgeousone.netherview.blocktype;

import org.bukkit.util.Vector;

public enum Axis {
	
	X(new Vector(0, 0, 1)),
	Z(new Vector( 1, 0, 0));
	
	private Vector normal;
	private Vector crossNormal;
	
	Axis(Vector normal) {
		this.normal = normal;
		this.crossNormal = normal.clone().crossProduct(new Vector(0, 1, 0));
	}
	
	public Vector getNormal() {
		return normal.clone();
	}
	
	public Vector getCrossNormal() {
		return crossNormal.clone();
	}
}
