package me.gorgeousone.netherview.blocktype;

import org.bukkit.util.Vector;

public enum Axis {
	
	X(new Vector(0, 0, 1), new Vector(1, 0, 0)),
	Z(new Vector(1, 0, 0), new Vector(0, 0, 1));
	
	private Vector normal;
	private Vector crossNormal;
	
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
