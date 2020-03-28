package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

public class Rectangle {
	
	private Axis axis;
	private Vector pos;
	private Vector size;
	private Plane plane;
	
	public Rectangle(Axis axis, Vector pos, int width, int height) {
		this.axis = axis;
		this.pos = pos;
		this.plane = new Plane(pos, AxisUtils.getAxisPlaneNormal(axis));
		
		setSize(width, height);
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public Vector getMin() {
		return pos.clone();
	}
	
	public Vector getMax() {
		return pos.clone().add(size);
	}
	
	public int width() {
		if (axis == Axis.X)
			return size.getBlockX();
		else
			return size.getBlockZ();
	}
	
	public int height() {
		return size.getBlockY();
	}
	
	public void setSize(int width, int height) {
		size = new Vector(0, height, 0);
		
		if (axis == Axis.X)
			size.setX(width);
		else
			size.setZ(width);
	}
	
	public Vector getSomewhatOfACenter() {
		return pos.clone().add(size.clone().multiply(0.5));
	}
	
	public void translate(Vector delta) {
		pos.add(delta);
	}
	
	public Plane getPlane() {
		return plane;
	}
	
	public boolean contains(Vector pointInPlane) {
		
		Vector min = getMin();
		Vector max = getMax();
		
		double pointY = pointInPlane.getY();
		
		if (pointY < min.getY() || pointY > max.getY())
			return false;
		
		switch (axis) {
			case X:
				double pointX = pointInPlane.getX();
				return pointX >= min.getX() && pointX <= max.getX();
			case Z:
				double pointZ = pointInPlane.getZ();
				return pointZ >= min.getZ() && pointZ <= max.getZ();
			default:
				return false;
		}
	}
	
	@Override
	public Rectangle clone() {
		return new Rectangle(axis, pos, width(), height());
	}
}
