package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.util.Vector;

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
	
	public void setTranslation(BlockVec delta) {
		this.translation = delta;
	}
	
	public void translate(BlockVec delta) {
		this.translation.add(delta);
	}
	
	public void setRotCenter(BlockVec rotCenter) {
		this.rotCenter = rotCenter;
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
	
	public Vector transformVec(Vector vec) {
		
		
		vec.subtract(rotCenter.toVector());
		rotateVec(vec);
		
		return vec.add(rotCenter.toVector()).add(translation.toVector());
	}
	
	private void rotateVec(Vector relativeVec) {
		
		double transX = relativeVec.getX();
		double transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
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