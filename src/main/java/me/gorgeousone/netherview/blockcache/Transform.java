package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.BlockVec;

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
	
	public boolean isRotY0Deg() {
		return rotYMatrix[0][0] == 1;
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
	
	//	public BlockType rotateData(BlockType blockType) {
	//
	//		if (isRotY0Deg())
	//			return blockType;
	//
	//		int rotInQuarterTurns = getRotationInQuarterTurns();
	//
	//		if (blockType instanceof Orientable) {
	//
	//			if (isRotY180Deg())
	//				return blockType;
	//
	//			Orientable orientable = (Orientable) blockType;
	//
	//			if (orientable.getAxis() != Axis.Y)
	//				orientable.setAxis(orientable.getAxis() == Axis.X ? Axis.Z : Axis.X);
	//
	//		} else if (blockType instanceof Directional) {
	//
	//			Directional directional = (Directional) blockType;
	//			directional.setFacing(FacingUtils.getRotatedFace(directional.getFacing(), rotInQuarterTurns));
	//
	//		} else if (blockType instanceof Rotatable) {
	//
	//			Rotatable rotatable = (Rotatable) blockType;
	//			rotatable.setRotation(FacingUtils.getRotatedFace(rotatable.getRotation(), rotInQuarterTurns));
	//
	//		} else if (blockType instanceof MultipleFacing) {
	//
	//			MultipleFacing multiFacing = (MultipleFacing) blockType;
	//			Map<BlockFace, Boolean> facings = new HashMap<>();
	//
	//			for (BlockFace face : multiFacing.getAllowedFaces()) {
	//				if (FacingUtils.isRotatableFace(face))
	//					facings.put(face, multiFacing.hasFace(face));
	//			}
	//
	//			for (BlockFace face : facings.keySet())
	//				multiFacing.setFace(FacingUtils.getRotatedFace(face, rotInQuarterTurns), facings.get(face));
	//
	//		} else if (blockType instanceof RedstoneWire) {
	//
	//			RedstoneWire wire = (RedstoneWire) blockType;
	//			Map<BlockFace, RedstoneWire.Connection> connections = new HashMap<>();
	//
	//			for (BlockFace face : wire.getAllowedFaces()) {
	//				if (FacingUtils.isRotatableFace(face))
	//					connections.put(face, wire.getFace(face));
	//			}
	//
	//			for (BlockFace face : connections.keySet())
	//				wire.setFace(FacingUtils.getRotatedFace(face, rotInQuarterTurns), connections.get(face));
	//		}
	//
	//		return blockType;
	//	}
	
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