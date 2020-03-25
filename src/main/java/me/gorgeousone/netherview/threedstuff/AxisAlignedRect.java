package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

public class AxisAlignedRect {
	
	private Axis axis;
	private Vector min;
	private Vector max;
	private Plane plane;
	
	public AxisAlignedRect(Axis axis, Vector min, Vector max) {
		this.axis = axis;
		this.min = min;
		this.max = max;
		this.plane = new Plane(min, AxisUtils.getAsVector(axis));
	}
	
	public void translate(Vector delta) {
		min.add(delta);
		max.add(delta);
	}
	
	public Plane getPlane() {
		return plane;
	}
	
	public boolean rectContains(Vector pointInPlane) {
		
		double pointY = pointInPlane.getY();
		
		switch (axis) {
			case X:
				double pointX = pointInPlane.getX();
				return pointX >= min.getX() && pointX <= max.getX() &&
				       pointY >= min.getY() && pointY <= max.getY();
			case Z:
				double pointZ = pointInPlane.getZ();
				return pointZ >= min.getX() && pointZ <= max.getX() &&
				       pointY >= min.getY() && pointY <= max.getY();
			default:
				return false;
		}
	}
}
