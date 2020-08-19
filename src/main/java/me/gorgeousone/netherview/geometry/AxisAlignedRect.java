package me.gorgeousone.netherview.geometry;

import me.gorgeousone.netherview.wrapping.Axis;
import org.bukkit.util.Vector;

/**
 * A rectangle used to describe the size of a nether portal (the part without the frame) or used as near plane in a view frustum.
 */
public class AxisAlignedRect {
	
	private final Axis axis;
	private final Vector pos;
	private final Plane plane;
	
	private double width;
	private double height;
	
	public AxisAlignedRect(Axis axis, Vector pos, double width, double height) {
		
		this.axis = axis;
		this.pos = pos.clone();
		
		if (axis == Axis.X) {
			plane = new Plane(pos, new Vector(0, 0, 1));
		} else {
			plane = new Plane(pos, new Vector(1, 0, 0));
		}
		
		setSize(width, height);
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public Vector getMin() {
		return pos.clone();
	}
	
	public Vector getMax() {
		return pos.clone().add(new Vector(
				axis == Axis.X ? width : 0,
				height,
				axis == Axis.Z ? width : 0));
	}
	
	public double width() {
		return width;
	}
	
	public double height() {
		return height;
	}
	
	public void setSize(double width, double height) {
		this.width = width;
		this.height = height;
	}
	
	public AxisAlignedRect translate(Vector delta) {
		pos.add(delta);
		plane.translate(delta);
		return this;
	}
	
	public Plane getPlane() {
		return plane;
	}
	
	public boolean contains(Vector pointInPlane) {
		
		Vector min = getMin();
		Vector max = getMax();
		
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
		return new AxisAlignedRect(getAxis(), getMin(), width(), height());
	}
}