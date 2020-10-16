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
		
		if (axis == Axis.X) {
			plane = new Plane(min, new Vector(0, 0, 1));
			
			if (min.getZ() != max.getZ()) {
				throw new IllegalArgumentException("Z coordinates of x aligned portal must be equal");
			}
		} else {
			plane = new Plane(min, new Vector(1, 0, 0));
			
			if (min.getX() != max.getX()) {
				throw new IllegalArgumentException("Z coordinates of x aligned portal must be equal");
			}
		}
		
		if (height() < 0 || width() < 0) {
			throw new IllegalArgumentException("Rectangle maximum coordinates must be greater than minimum coordinates");
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
	
	@Override
	public AxisAlignedRect clone() {
		return new AxisAlignedRect(getAxis(), getMin(), getMax());
	}
}