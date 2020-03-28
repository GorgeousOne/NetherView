package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class Transform {
	
	private Vector translation;
	private Vector rotCenter;
	private int[][] rotationY;
	
	public Transform() {
		translation = new Vector();
		rotCenter = new Vector();
		rotationY = new int[][]{{1, 0}, {0, 1}};
	}
	
	protected Transform(Vector translation, Vector rotCenter, int[][] rotationY) {
		this.translation = translation;
		this.rotCenter = rotCenter;
		this.rotationY = rotationY;
	}
	
	public void setTranslation(Vector delta) {
		this.translation = delta;
	}
	
	public void setRotCenter(Vector rotCenter) {
		this.rotCenter = rotCenter;
	}
	
	public void setRotY90DegRight() {
		rotationY[0][0] = 0;
		rotationY[0][1] = -1;
		rotationY[1][0] = 1;
		rotationY[1][1] = 0;
	}
	
	public void setRotY90DegLeft() {
		rotationY[0][0] = 0;
		rotationY[0][1] = 1;
		rotationY[1][0] = -1;
		rotationY[1][1] = 0;
	}
	
	public Vector getTransformed(Vector point) {
		
		Vector transformed = point.clone();
		
		transformed.subtract(rotCenter);
		rotate(transformed);
		
		return transformed.add(rotCenter).add(translation);
	}
	
	public Vector rotate(Vector relativePoint) {
		
		double transX = relativePoint.getX();
		double transZ = relativePoint.getZ();
		
		relativePoint.setX(rotationY[0][0] * transX + rotationY[0][1] * transZ);
		relativePoint.setZ(rotationY[1][0] * transX + rotationY[1][1] * transZ);
		
		return relativePoint;
	}
	
	public Transform invert() {
		
		rotCenter.add(translation);
		translation.multiply(-1);
		
		rotationY[0][1] *= -1;
		rotationY[1][0] *= -1;
		
		return this;
	}
	
	@Override
	public Transform clone() {
		return new Transform(translation.clone(), rotCenter.clone(), rotationY.clone());
	}
}