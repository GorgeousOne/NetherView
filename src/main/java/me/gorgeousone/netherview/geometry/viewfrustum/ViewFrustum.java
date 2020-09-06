package me.gorgeousone.netherview.geometry.viewfrustum;

import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Line;
import me.gorgeousone.netherview.geometry.Plane;
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * A viewing frustum for detecting the blocks of a projection cache that can be seen through a portal frame.
 * It is a frustum with the specific condition that near and far plane are rectangles either aligned to the x or z axis.
 */
public class ViewFrustum {
	
	private final Vector viewPoint;
	private final AxisAlignedRect nearPlaneRect;
	private final AxisAlignedRect farPlaneRect;
	
	private final int frustumLength;
	private final Vector frustumFacing;
	
	public ViewFrustum(Vector viewPoint, AxisAlignedRect nearPlane, int frustumLength) {
		
		this.viewPoint = viewPoint;
		this.nearPlaneRect = nearPlane;
		this.frustumLength = frustumLength;
		
		this.frustumFacing = createFrustumFacing();
		this.farPlaneRect = createFarPlaneRect();
	}
	
	public Vector getViewPoint() {
		return viewPoint.clone();
	}
	
	public AxisAlignedRect getNearPlaneRect() {
		return nearPlaneRect.clone();
	}
	
	public AxisAlignedRect getFarPlaneRect() {
		return farPlaneRect.clone();
	}
	
	public boolean contains(Vector point) {
		
		DefinedLine lineOfView = new DefinedLine(viewPoint, point);
		Vector pointInNearPlane = nearPlaneRect.getPlane().getIntersection(lineOfView);
		
		return pointInNearPlane != null && nearPlaneRect.contains(pointInNearPlane);
	}
	
	/**
	 * Returns true if any vertex of the block at the given position intersects the frustum
	 */
	public boolean containsBlock(Vector blockPos) {
		
		for (int dx = 0; dx <= 1; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = 0; dz <= 1; dz++) {
					
					if (contains(blockPos.clone().add(new Vector(dy, dy, dz)))) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public int getLength() {
		return frustumLength;
	}
	
	private Vector createFrustumFacing() {
		
		//take the near plane's normal as the facing of the frustum
		Vector frustumFacing = nearPlaneRect.getNormal();
		
		//but make it face in the opposite direction of where the view point
		Vector relViewPoint = viewPoint.clone().subtract(nearPlaneRect.getMin());
		frustumFacing.multiply(-Math.signum(frustumFacing.dot(relViewPoint)));
		
		return frustumFacing;
	}
	
	private AxisAlignedRect createFarPlaneRect() {
		
		Vector nearPlaneOrigin = nearPlaneRect.getMin();
		Vector nearPlaneNormal = nearPlaneRect.getNormal();
		
		//calculate a vector representing the distance between near plane and far plane
		Vector farPlaneOffset = frustumFacing.clone().multiply(frustumLength);
		
		//calculate a far plane parallel to the near plane with the correct offset
		Vector farPlaneOrigin = nearPlaneOrigin.clone().add(farPlaneOffset);
		Plane farPlane = new Plane(farPlaneOrigin, nearPlaneNormal);
		
		//create two lines for mapping the vertices of the near rect onto the far plane
		Line farRectMinLine = new Line(viewPoint, nearPlaneOrigin);
		Line farRectMaxLine = new Line(viewPoint, nearPlaneRect.getMax());
		
		return new AxisAlignedRect(
				nearPlaneRect.getAxis(),
				farPlane.getIntersection(farRectMinLine),
				farPlane.getIntersection(farRectMaxLine));
	}
	
	/**
	 * Returns a map of all blocks from a projection cache visible with this frustum.
	 */
	public Map<BlockVec, BlockType> getContainedBlocks(ProjectionCache projection) {
		
		AxisAlignedRect startLayer;
		AxisAlignedRect endLayer;
		
		if (frustumFacing.getX() == -1 || frustumFacing.getZ() == -1) {
			startLayer = farPlaneRect;
			endLayer = nearPlaneRect;
			
		} else {
			startLayer = nearPlaneRect;
			endLayer = farPlaneRect;
		}
		
		if (nearPlaneRect.getAxis() == Axis.X) {
			return getBlocksInXAlignedFrustum(projection, startLayer, endLayer);
		} else {
			return getBlocksInZAlignedFrustum(projection, startLayer, endLayer);
		}
	}
	
	private Map<BlockVec, BlockType> getBlocksInXAlignedFrustum(ProjectionCache projection,
	                                                            AxisAlignedRect startLayer,
	                                                            AxisAlignedRect endLayer) {
		
		Vector iterationMaxPoint = endLayer.getMax();
		Vector currentLayerMinPoint = startLayer.getMin();
		Vector currentLayerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		for (int layerZ = (int) Math.round(currentLayerMinPoint.getZ()); layerZ <= iterationMaxPoint.getZ(); layerZ++) {
			
			int layerMax = currentLayerMaxPoint.getBlockX();
			boolean isFirstColumn = true;
			boolean isLastColumn = false;
			
			for (int columnX = (int) Math.ceil(currentLayerMinPoint.getX()); isFirstColumn || columnX <= layerMax; columnX++) {
				
				int columnMax = currentLayerMaxPoint.getBlockY();
				boolean isFirstRow = true;
				boolean isLastRow = false;
				
				if (columnX == layerMax) {
					isLastColumn = true;
				}
				
				for (int rowY = (int) Math.ceil(currentLayerMinPoint.getY()); isFirstRow || rowY <= columnMax; rowY++) {
					
					if (rowY == columnMax) {
						isLastRow = true;
					}
					
					//since the iteration only passes every block at one point, the surrounding blocks have to be added on edges for accurately determining all blocks
					if (isFirstColumn || isLastColumn || isFirstRow || isLastRow) {
						addSurroundingBlocks(columnX, rowY, layerZ, projection, blocksInFrustum);
					} else {
						addBlock(columnX, rowY, layerZ, projection, blocksInFrustum);
					}
					
					isFirstRow = false;
				}
				isFirstColumn = false;
			}
			
			currentLayerMinPoint.add(layerMinPointStep);
			currentLayerMaxPoint.add(layerMaxPointStep);
		}
		
		return blocksInFrustum;
	}
	
	private Map<BlockVec, BlockType> getBlocksInZAlignedFrustum(ProjectionCache projection,
	                                                            AxisAlignedRect startLayer,
	                                                            AxisAlignedRect endLayer) {
		
		Vector iterationMaxPoint = endLayer.getMax();
		Vector currentLayerMinPoint = startLayer.getMin();
		Vector currentLayerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		for (int layerX = (int) Math.round(currentLayerMinPoint.getX()); layerX <= iterationMaxPoint.getX(); layerX++) {
			
			int layerMax = currentLayerMaxPoint.getBlockZ();
			boolean isFirstColumn = true;
			boolean isLastColumn = false;
			
			for (int columnZ = (int) Math.ceil(currentLayerMinPoint.getZ()); isFirstColumn || columnZ <= layerMax; columnZ++) {
				
				int columnMax = currentLayerMaxPoint.getBlockY();
				boolean isFirstRow = true;
				boolean isLastRow = false;
				
				if (columnZ == layerMax) {
					isLastColumn = true;
				}
				
				for (int rowY = (int) Math.ceil(currentLayerMinPoint.getY()); isFirstRow || rowY <= columnMax; rowY++) {
					
					if (rowY == columnMax) {
						isLastRow = true;
					}
					
					if (isFirstColumn || isLastColumn || isFirstRow || isLastRow) {
						addSurroundingBlocks(layerX, rowY, columnZ, projection, blocksInFrustum);
					} else {
						addBlock(layerX, rowY, columnZ, projection, blocksInFrustum);
					}
					
					isFirstRow = false;
				}
				isFirstColumn = false;
			}
			
			currentLayerMinPoint.add(layerMinPointStep);
			currentLayerMaxPoint.add(layerMaxPointStep);
		}
		
		return blocksInFrustum;
	}
	
	/**
	 * Adds a blocks from the projection cache to the visible blocks in the frustum.
	 */
	private void addBlock(int x,
	                      int y,
	                      int z,
	                      ProjectionCache projection,
	                      Map<BlockVec, BlockType> blocksInFrustum) {
		
		BlockType blockType = projection.getBlockTypeAt(x, y, z);
		
		if (blockType != null) {
			blocksInFrustum.put(new BlockVec(x, y, z), blockType);
		}
	}
	
	/**
	 * Adds the 8 blocks from the projection cache around a block location to the map of visible blocks in the frustum.
	 */
	private void addSurroundingBlocks(int x,
	                                  int y,
	                                  int z,
	                                  ProjectionCache projection,
	                                  Map<BlockVec, BlockType> blocksInFrustum) {
		
		for (int dx = -1; dx <= 0; dx++) {
			for (int dy = -1; dy <= 0; dy++) {
				for (int dz = -1; dz <= 0; dz++) {
					
					BlockType blockType = projection.getBlockTypeAt(x + dx, y + dy, z + dz);
					
					if (blockType != null) {
						//trying to reduce the object creation (the BlockVec)
						blocksInFrustum.put(new BlockVec(x + dx, y + dy, z + dz), blockType);
					}
				}
			}
		}
	}
}