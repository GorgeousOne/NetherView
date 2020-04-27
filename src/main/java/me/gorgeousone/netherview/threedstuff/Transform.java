package me.gorgeousone.netherview.threedstuff;

import me.gorgeousone.netherview.blockcache.BlockVec;

public class Transform {
	
	private BlockVec translation;
	private BlockVec rotCenter;
	private int[][] rotationY;
	
	public Transform() {
		translation = new BlockVec();
		rotCenter = new BlockVec();
		rotationY = new int[][]{{1, 0}, {0, 1}};
	}
	
	protected Transform(BlockVec translation, BlockVec rotCenter, int[][] rotationY) {
		this.translation = translation;
		this.rotCenter = rotCenter;
		this.rotationY = rotationY;
	}
	
	public void setTranslation(BlockVec delta) {
		this.translation = delta;
	}
	
	public void setRotCenter(BlockVec rotCenter) {
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
	
	public void setRotY180Deg() {
		rotationY[0][0] = -1;
		rotationY[0][1] = 0;
		rotationY[1][0] = 0;
		rotationY[1][1] = -1;
	}
	
	public BlockVec getTransformed(BlockVec point) {
		
		BlockVec transformed = point.clone();
		
		transformed.subtract(rotCenter);
		rotate(transformed);
		
		return transformed.add(rotCenter).add(translation);
	}
	
	public BlockVec rotate(BlockVec relativePoint) {
		
		int transX = relativePoint.getX();
		int transZ = relativePoint.getZ();
		
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
		return new Transform(translation.clone(), rotCenter.clone(), cloneRotY());
	}
	
	private int[][] cloneRotY() {
		return new int[][] {
				{rotationY[0][0], rotationY[0][1]},
				{rotationY[1][0], rotationY[1][1]}
		};
	}
}