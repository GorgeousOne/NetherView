package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.wrapping.Axis;
import org.bukkit.util.Vector;

/**
 * A class holding the relative translation and rotation between two portals and applying it to BlockVecs.
 */
public class Transform {
	
	private BlockVec translation;
	private BlockVec rotCenter;
	private final int[][] rotYMatrix;
	
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
	
	public BlockVec transformVec(BlockVec vec) {
		
		vec.subtract(rotCenter);
		rotateVec(vec);
		return vec.add(rotCenter).add(translation);
	}
	
	public Vector transformVec(Vector vec) {
		
		Vector vecRotCenter = rotCenter.toVector();
		vec.subtract(vecRotCenter);
		rotateVec(vec);
		return vec.add(vecRotCenter).add(translation.toVector());
	}
	
	private void rotateVec(BlockVec relativeVec) {
		
		int transX = relativeVec.getX();
		int transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
	}
	
	private void rotateVec(Vector relativeVec) {
		
		double transX = relativeVec.getX();
		double transZ = relativeVec.getZ();
		
		relativeVec.setX(rotYMatrix[0][0] * transX + rotYMatrix[0][1] * transZ);
		relativeVec.setZ(rotYMatrix[1][0] * transX + rotYMatrix[1][1] * transZ);
	}
	
	public ViewFrustum getTransformedFrustum(ViewFrustum frustum) {
		
		return new ViewFrustum(
				transformVec(frustum.getViewPoint()),
				getTransformedRect(frustum.getNearPlaneRect()),
				frustum.getLength());
	}
	
	public AxisAlignedRect getTransformedRect(AxisAlignedRect rect) {
	
		return new AxisAlignedRect(
				getRotatedAxis(rect.getAxis()),
				transformVec(rect.getMin()),
				transformVec(rect.getMax()));
	}
	
	public Axis getRotatedAxis(Axis axis) {
		return axis == Axis.X ^ (isRotY0Deg() || isRotY180Deg()) ? Axis.Z : Axis.X;
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
	
	public void invertRotation() {
		
		rotYMatrix[0][0] *= -1;
		rotYMatrix[0][1] *= -1;
		rotYMatrix[1][0] *= -1;
		rotYMatrix[1][1] *= -1;
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