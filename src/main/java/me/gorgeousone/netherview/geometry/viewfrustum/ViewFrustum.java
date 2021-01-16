package me.gorgeousone.netherview.geometry.viewfrustum;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Line;
import me.gorgeousone.netherview.geometry.Plane;
import me.gorgeousone.netherview.wrapper.Axis;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
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
		Vector frustumFacing = nearPlaneRect.getAxis().getNormal();
		
		//but make it face in the opposite direction of where the view point
		Vector relViewPoint = viewPoint.clone().subtract(nearPlaneRect.getMin());
		frustumFacing.multiply(-Math.signum(frustumFacing.dot(relViewPoint)));
		
		return frustumFacing;
	}
	
	private AxisAlignedRect createFarPlaneRect() {
		
		Vector nearPlaneOrigin = nearPlaneRect.getMin();
		Vector nearPlaneNormal = nearPlaneRect.getAxis().getNormal();
		
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
	public Map<BlockVec, BlockType> getContainedBlocks(BlockCache projection) {
		
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
	
	/**
	 * Iterates through all coordinates of the the block cache that the frustum contains and returns a map of all blocks in that area.
	 */
	private Map<BlockVec, BlockType> getBlocksInXAlignedFrustum(BlockCache blockCache,
	                                                            AxisAlignedRect startLayer,
	                                                            AxisAlignedRect endLayer) {
		
		//I get it. It's long and not comprehensible, but it has to be all in 1 method for max efficiency
		Vector layerMinPoint = startLayer.getMin();
		Vector layerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		//imagine cutting a frustum in single block layers along the z axis
		//these are the minimum and maximum coordinates for each of these layers
		int[] minXs = new int[frustumLength + 1];
		int[] minYs = new int[frustumLength + 1];
		int[] maxXs = new int[frustumLength + 1];
		int[] maxYs = new int[frustumLength + 1];
		
		for (int i = 0; i <= frustumLength; i++) {
			
			minXs[i] = layerMinPoint.getBlockX();
			minYs[i] = layerMinPoint.getBlockY();
			maxXs[i] = layerMaxPoint.getBlockX();
			maxYs[i] = layerMaxPoint.getBlockY();
			
			layerMinPoint.add(layerMinPointStep);
			layerMaxPoint.add(layerMaxPointStep);
		}
		
		//since blocks often intersect a view frustum with other vertices than the coordinates can tell these layer offsets are created
		//if the frustum is widening in any direction the coordinates of a wider layer have to be included into the previous thinner layer.
		//the offsets will be used to determine when to use the bounds of a next greater layer (layer index + 1) or not (layer index + 0)
		int layerOffsetMinX = (int) Math.signum(layerMinPointStep.getX()) == -1 ? 1 : 0;
		int layerOffsetMinY = (int) Math.signum(layerMinPointStep.getY()) == -1 ? 1 : 0;
		int layerOffsetMaxX = (int) Math.signum(layerMaxPointStep.getX()) == 1 ? 1 : 0;
		int layerOffsetMaxY = (int) Math.signum(layerMaxPointStep.getY()) == 1 ? 1 : 0;
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		int startZ = (int) Math.round(startLayer.getMin().getZ());
		
		//now this is just the iteration I was talking about all the time
		for (int i = 0; i < frustumLength; i++) {
			
			int layerZ = startZ + i;
			int startX = minXs[i + layerOffsetMinX];
			int startY = minYs[i + layerOffsetMinY];
			int endX = maxXs[i + layerOffsetMaxX];
			int endY = maxYs[i + layerOffsetMaxY];
			
			for (int row = startX; row <= endX; row++) {
				for (int columnY = startY; columnY <= endY; columnY++) {
					addBlock(row, columnY, layerZ, blockCache, blocksInFrustum);
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	private Map<BlockVec, BlockType> getBlocksInZAlignedFrustum(BlockCache blockCache,
	                                                            AxisAlignedRect startLayer,
	                                                            AxisAlignedRect endLayer) {
		
		Vector layerMinPoint = startLayer.getMin();
		Vector layerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		int[] minZs = new int[frustumLength + 1];
		int[] minYs = new int[frustumLength + 1];
		int[] maxZs = new int[frustumLength + 1];
		int[] maxYs = new int[frustumLength + 1];
		
		for (int i = 0; i <= frustumLength; i++) {
			
			minZs[i] = layerMinPoint.getBlockZ();
			minYs[i] = layerMinPoint.getBlockY();
			maxZs[i] = layerMaxPoint.getBlockZ();
			maxYs[i] = layerMaxPoint.getBlockY();
			
			layerMinPoint.add(layerMinPointStep);
			layerMaxPoint.add(layerMaxPointStep);
		}
		
		int offMinZ = (int) Math.signum(layerMinPointStep.getZ()) == -1 ? 1 : 0;
		int offMinY = (int) Math.signum(layerMinPointStep.getY()) == -1 ? 1 : 0;
		int offMaxZ = (int) Math.signum(layerMaxPointStep.getZ()) == 1 ? 1 : 0;
		int offMaxY = (int) Math.signum(layerMaxPointStep.getY()) == 1 ? 1 : 0;
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		int startX = (int) Math.round(startLayer.getMin().getX());
		
		for (int i = 0; i < frustumLength; i++) {
			
			int layerX = startX + i;
			int startZ = minZs[i + offMinZ];
			int startY = minYs[i + offMinY];
			int endZ = maxZs[i + offMaxZ];
			int endY = maxYs[i + offMaxY];
			
			for (int rowZ = startZ; rowZ <= endZ; rowZ++) {
				for (int columnY = startY; columnY <= endY; columnY++) {
					addBlock(layerX, columnY, rowZ, blockCache, blocksInFrustum);
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	private Map<BlockVec, BlockType> getBlocksInYAlignedFrustum(BlockCache blockCache,
	                                                            AxisAlignedRect startLayer,
	                                                            AxisAlignedRect endLayer) {
		
		Vector layerMinPoint = startLayer.getMin();
		Vector layerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		int[] minZs = new int[frustumLength + 1];
		int[] minXs = new int[frustumLength + 1];
		int[] maxZs = new int[frustumLength + 1];
		int[] maxXs = new int[frustumLength + 1];
		
		for (int i = 0; i <= frustumLength; i++) {
			
			minZs[i] = layerMinPoint.getBlockZ();
			minXs[i] = layerMinPoint.getBlockY();
			maxZs[i] = layerMaxPoint.getBlockZ();
			maxXs[i] = layerMaxPoint.getBlockY();
			
			layerMinPoint.add(layerMinPointStep);
			layerMaxPoint.add(layerMaxPointStep);
		}
		
		int offMinZ = (int) Math.signum(layerMinPointStep.getZ()) == -1 ? 1 : 0;
		int offMinX = (int) Math.signum(layerMinPointStep.getX()) == -1 ? 1 : 0;
		int offMaxZ = (int) Math.signum(layerMaxPointStep.getZ()) == 1 ? 1 : 0;
		int offMaxX = (int) Math.signum(layerMaxPointStep.getX()) == 1 ? 1 : 0;
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		int startY = (int) Math.round(startLayer.getMin().getX());
		
		for (int i = 0; i < frustumLength; i++) {
			
			int layerY = startY + i;
			int startZ = minZs[i + offMinZ];
			int startX = minXs[i + offMinX];
			int endZ = maxZs[i + offMaxZ];
			int endX = maxXs[i + offMaxX];
			
			for (int rowX = startZ; rowX <= endZ; rowX++) {
				for (int columnZ = startX; columnZ <= endX; columnZ++) {
					addBlock(rowX, layerY, columnZ, blockCache, blocksInFrustum);
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	
	/**
	 * Adds a blocks from the projection cache to the visible blocks in the frustum.
	 */
	private void addBlock(int x,
	                      int y,
	                      int z,
	                      BlockCache projection,
	                      Map<BlockVec, BlockType> blocksInFrustum) {
		
		BlockType blockType = projection.getBlockTypeAt(x, y, z);
		
		if (blockType != null) {
			blocksInFrustum.put(new BlockVec(x, y, z), blockType);
		}
	}
}