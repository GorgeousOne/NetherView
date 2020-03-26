package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class PortalRectangle {
	
	private Axis axis;
	private Vector min;
	private Vector max;
	private Plane plane;
	
	public PortalRectangle(Axis axis, Vector min, Vector max) {
		this.axis = axis;
		this.min = min;
		this.max = max;
		this.plane = new Plane(min, AxisUtils.getAxisPlaneNormal(axis));
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public Vector getMin() {
		return min.clone();
	}
	
	public Vector getMax() {
		return max.clone();
	}
	
	public int width() {
		if (axis == Axis.X)
			return max.getBlockX() - min.getBlockX();
		else
			return max.getBlockZ() - min.getBlockZ();
	}
	
	public int height() {
		return max.getBlockY() - min.getBlockY();
	}
	
	public Vector getSomewhatOfACenter() {
		return min.clone().add(max.clone().subtract(min).multiply(0.5));
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
