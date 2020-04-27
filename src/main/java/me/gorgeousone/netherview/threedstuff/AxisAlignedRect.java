package me.gorgeousone.netherview.threedstuff;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

/**
 * A rectangle used to describe the size of a nether portal (the part without the frame) or used as near plane in a viewing frustum.
 */
public class AxisAlignedRect {
	
	private Axis axis;
	private Vector pos;
	private double width;
	private double height;
	private Plane plane;
	
	public AxisAlignedRect(Axis axis, Vector pos, double width, double height) {
		
		if(axis == Axis.Y)
			throw new IllegalArgumentException("Why would you want to use Axis.Y for a rectangle?");
		
		this.axis = axis;
		this.pos = pos.clone();
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
		
		if (pointY < min.getY() || pointY > max.getY())
			return false;
		
		if(axis == Axis.X) {
			double pointX = pointInPlane.getX();
			return pointX >= min.getX() && pointX <= max.getX();
			
		}else {
			double pointZ = pointInPlane.getZ();
			return pointZ >= min.getZ() && pointZ <= max.getZ();
		}
	}
	
	@Override
	public AxisAlignedRect clone() {
		return new AxisAlignedRect(getAxis(), getMin(), width(), height());
	}
}