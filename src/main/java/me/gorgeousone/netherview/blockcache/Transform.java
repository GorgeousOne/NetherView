package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.BlockVec;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * A class holding the relative translation and rotation between two portals and applying it to BlockVecs.
 */
public class Transform {
	
	private Vector translation;
	private Vector rotCenter;
	private final int[][] rotYMatrix;
	
	public Transform() {
		
		translation = new Vector();
		rotCenter = new Vector();
		rotYMatrix = new int[][]{{1, 0}, {0, 1}};
	}
	
	protected Transform(BlockVec translation, BlockVec rotCenter, int[][] rotationY) {
		
		this.translation = translation.toVector();
		this.rotCenter = rotCenter.toVector().add(new Vector(0.5, 0, 0.5));
		this.rotYMatrix = rotationY;
	}
	
	protected Transform(Vector translation, Vector rotCenter, int[][] rotationY) {
		
		this.translation = translation.clone();
		this.rotCenter = rotCenter.clone();
		this.rotYMatrix = rotationY;
	}
	
	public Vector getTranslation() {
		return translation.clone();
	}
	
	public Transform setTranslation(Vector pos) {
		this.translation = pos.clone();
		return this;
	}
	
	public Transform setTranslation(BlockVec pos) {
		this.translation = pos.toVector().add(new Vector(0.5, 0, 0.5));
		return this;
	}
	
	public Transform translate(double dx, double dy, double dz) {
		this.translation.add(new Vector(dx, dy, dz));
		return this;
	}
	
	public Transform setRotCenter(BlockVec rotCenter) {
		this.rotCenter = rotCenter.toVector();
		return this;
	}
	
	public Transform translateRotCenter(double dx, double dy, double dz) {
		this.rotCenter.add(new Vector(dx, dy, dz));
		return this;
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
	
	public boolean isRotY0Deg() {
		return rotYMatrix[0][0] == 1;
	}
	
	public Transform setRotY90DegRight() {
		
		rotYMatrix[0][0] = 0;
		rotYMatrix[0][1] = -1;
		rotYMatrix[1][0] = 1;
		rotYMatrix[1][1] = 0;
		return this;
	}
	
	public Transform setRotY90DegLeft() {
		
		rotYMatrix[0][0] = 0;
		rotYMatrix[0][1] = 1;
		rotYMatrix[1][0] = -1;
		rotYMatrix[1][1] = 0;
		return this;
	}
	
	public Transform setRotY180Deg() {
		
		rotYMatrix[0][0] = -1;
		rotYMatrix[0][1] = 0;
		rotYMatrix[1][0] = 0;
		rotYMatrix[1][1] = -1;
		return this;
	}
	
	public BlockVec transformVec(BlockVec blockVec) {
		
		BlockVec blockRotCenter = new BlockVec(rotCenter);
		blockVec.subtract(blockRotCenter);
		rotateVec(blockVec);
		blockVec.add(blockRotCenter);
		
		return blockVec.add(new BlockVec(translation));
	}
	
	public Vector transformVec(Vector vec) {
		
		Vector vecRotCenter = rotCenter;
		vec.subtract(vecRotCenter);
		rotateVec(vec);
		return vec.add(vecRotCenter).add(translation);
	}
	
	public Location transformLoc(Location loc) {
		
		if (!isRotY0Deg()) {
			
			loc.subtract(rotCenter);
			rotateLoc(loc);
			loc.add(rotCenter);
		}
		
		if (translation.lengthSquared() != 0) {
			loc.add(translation);
		}
		
		return loc;
	}
	
	private BlockVec rotateVec(BlockVec relativeVec) {
		
		int transX = relativeVec.getX();
		int transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
		return relativeVec;
	}
	
	public Vector rotateVec(Vector relativeVec) {
		
		double transX = relativeVec.getX();
		double transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
		return relativeVec;
	}
	
	public Location rotateLoc(Location relativeLoc) {
		
		double transX = relativeLoc.getX();
		double transZ = relativeLoc.getZ();
		
		relativeLoc.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeLoc.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
		
		relativeLoc.setYaw(rotateYaw(relativeLoc.getYaw()));
		return relativeLoc;
	}
	
	public float rotateYaw(float yaw) {
		
		float rotatedYaw = yaw + getQuarterTurns() * 90;
		rotatedYaw %= 360;
		
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
		invertRotation();
		
		return this;
	}
	
	public Transform invertRotation() {
		
		rotYMatrix[0][0] *= -1;
		rotYMatrix[0][1] *= -1;
		rotYMatrix[1][0] *= -1;
		rotYMatrix[1][1] *= -1;
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