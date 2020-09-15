package me.gorgeousone.netherview.geometry;

import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.util.Vector;

/**
 * A rectangle used to describe the size of a nether portal (the part without the frame) or used as near plane in a view frustum.
 */
public class AxisAlignedRect {
	
	private final Axis axis;
	private final Plane plane;
	
	private final Vector min;
	private final Vector max;
	
	public AxisAlignedRect(Axis axis, Vector min, Vector max) {
		
		this.axis = axis;
		this.min = min.clone();
		this.max = max.clone();
		
		if (height() < 0) {
			throw new IllegalArgumentException("Rectangle maximum y must be greater than minimum y");
		}
		
		if (axis == Axis.X) {
			plane = new Plane(min, new Vector(0, 0, 1));
		} else {
			plane = new Plane(min, new Vector(1, 0, 0));
		}
		
		if (width() < 0) {
			throw new IllegalArgumentException("Rectangle maximum must be greater than minimum");
		}
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
	
	public double width() {
		return axis == Axis.X ?
				max.getX() - min.getX() :
				max.getZ() - min.getZ();
	}
	
	public double height() {
		return max.getY() - min.getY();
	}
	
	public AxisAlignedRect translate(Vector delta) {
		min.add(delta);
		max.add(delta);
		plane.translate(delta);
		return this;
	}
	
	public Plane getPlane() {
		return plane;
	}
	
	public boolean contains(Vector pointInPlane) {
		
		double pointY = pointInPlane.getY();
		
		if (pointY < min.getY() || pointY > max.getY()) {
			return false;
		}
		
		if (axis == Axis.X) {
			double pointX = pointInPlane.getX();
			return pointX >= min.getX() && pointX <= max.getX();
			
		} else {
			double pointZ = pointInPlane.getZ();
			return pointZ >= min.getZ() && pointZ <= max.getZ();
		}
	}
	
	/**
	 * Returns a normal vector for the plane of the rectangle.
	 */
	public Vector getNormal() {
		return axis.getNormal();
	}
	
	/**
	 * Returns a vector 90° to the plane normal vector
	 */
	public Vector getCrossNormal() {
		return axis.getCrossNormal();
	}
	
	@Override
	public AxisAlignedRect clone() {
		return new AxisAlignedRect(getAxis(), getMin(), getMax());
	}
}