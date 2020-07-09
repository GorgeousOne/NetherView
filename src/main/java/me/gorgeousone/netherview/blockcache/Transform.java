package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.BlockVec;

/**
 * A class for storing a translation and rotation between two portals and applying it to BlockVecs.
 */
public class Transform {
	
	private BlockVec translation;
	private BlockVec rotCenter;
	private int[][] rotYMatrix;
	
	public Transform() {
		translation = new BlockVec();
		rotCenter = new BlockVec();
		rotYMatrix = new int[][]{{1, 0}, {0, 1}};
	}
	
	protected Transform(BlockVec translation, BlockVec rotCenter, int[][] rotationY) {
		this.translation = translation;
		this.rotCenter = rotCenter;
		this.rotYMatrix = rotationY;
	}
	
	public void setTranslation(BlockVec pos) {
		this.translation = pos.clone();
	}
	
	public void translate(BlockVec delta) {
		this.translation.add(delta);
	}
	
	public void setRotCenter(BlockVec rotCenter) {
		this.rotCenter = rotCenter.clone();
	}
	
	public void setRotY90DegRight() {
		rotYMatrix[0][0] = 0;
		rotYMatrix[0][1] = -1;
		rotYMatrix[1][0] = 1;
		rotYMatrix[1][1] = 0;
	}
	
	public void setRotY90DegLeft() {
		rotYMatrix[0][0] = 0;
		rotYMatrix[0][1] = 1;
		rotYMatrix[1][0] = -1;
		rotYMatrix[1][1] = 0;
	}
	
	public void setRotY180Deg() {
		rotYMatrix[0][0] = -1;
		rotYMatrix[0][1] = 0;
		rotYMatrix[1][0] = 0;
		rotYMatrix[1][1] = -1;
	}
	
	public boolean isRotY90DegRight() {
		return rotYMatrix[0][1] == -1;
	}
	
	public boolean isRotY90DegLeft() {
		return rotYMatrix[0][1] == 1;
	}
	
	public boolean isRotY180Deg() {
		return rotYMatrix[0][0] == -1;
	}
	
	public BlockVec transformVec(BlockVec vec) {
		
		vec.subtract(rotCenter);
		rotateVec(vec);
		
		return vec.add(rotCenter).add(translation);
	}
	
	private void rotateVec(BlockVec relativeVec) {
		
		int transX = relativeVec.getX();
		int transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
	}
	
	public float rotateYaw(float playerYaw) {
		
		float rotatedYaw = playerYaw + getQuarterTurns() * 90;
		
		if (Math.abs(rotatedYaw) > 360) {
			rotatedYaw -= Math.signum(rotatedYaw) * 360;
		}
		
		return rotatedYaw;
	}
	
	public int getQuarterTurns() {
		
		if (isRotY90DegLeft()) {
			return 3;
		} else if (isRotY180Deg()) {
			return 2;
		} else if (isRotY90DegRight()) {
			return 1;
		}
		
		return 0;
	}
	
	public Transform invert() {
		
		rotCenter.add(translation);
		translation.multiply(-1);
		
		rotYMatrix[0][1] *= -1;
		rotYMatrix[1][0] *= -1;
		
		return this;
	}
	
	@Override
	public Transform clone() {
		return new Transform(translation.clone(), rotCenter.clone(), cloneRotY());
	}
	
	private int[][] cloneRotY() {
		return new int[][]{
				{rotYMatrix[0][0], rotYMatrix[0][1]},
				{rotYMatrix[1][0], rotYMatrix[1][1]}
		};
	}
}