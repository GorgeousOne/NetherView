package me.gorgeousone.netherview.threedstuff.viewfrustum;

import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.Line;
import me.gorgeousone.netherview.threedstuff.Plane;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class ViewFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlaneRect;
	private AxisAlignedRect farPlaneRect;
	
	private int frustumLength;
	private Vector frustumFacing;
	
	public ViewFrustum(Vector viewPoint, AxisAlignedRect nearPlane, int frustumLength) {
		
		this.viewPoint = viewPoint;
		this.nearPlaneRect = nearPlane;
		this.frustumLength = frustumLength;
		
		createFrustumFacing();
		createFarPlaneRect();
	}
	
	public AxisAlignedRect getNearPlaneRect() {
		return nearPlaneRect;
	}
	
	public AxisAlignedRect getFarPlaneRect() {
		return farPlaneRect;
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
	
	private void createFrustumFacing() {
		
		//take the near plane's normal as the facing of the frustum
		this.frustumFacing = nearPlaneRect.getNormal();
		
		//but make it face in the opposite direction of where the view point
		Vector relViewPoint = viewPoint.clone().subtract(nearPlaneRect.getMin());
		frustumFacing.multiply(-Math.signum(frustumFacing.dot(relViewPoint)));
	}
	
	private void createFarPlaneRect() {
		
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
		
		Vector farRectMin = farPlane.getIntersection(farRectMinLine);
		Vector farRectMax = farPlane.getIntersection(farRectMaxLine);
		
		Vector rectDiameter = farRectMax.clone().subtract(farRectMin);
		double rectHeight = rectDiameter.getY();
		double rectWidth = rectDiameter.setY(0).length();
		
		farPlaneRect = new AxisAlignedRect(nearPlaneRect.getAxis(), farRectMin, rectWidth, rectHeight);
	}
	
	/**
	 * Returns a map of all blocks from a projection cache visible in this frustum.
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
		
		Vector iterationMaxPoint = endLayer.getMax();
		Vector currentLayerMinPoint = startLayer.getMin();
		Vector currentLayerMaxPoint = startLayer.getMax();
		
		Vector layerMinPointStep = endLayer.getMin().subtract(startLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxPointStep = endLayer.getMax().subtract(startLayer.getMax()).multiply(1d / frustumLength);
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		if (nearPlaneRect.getAxis() == Axis.X) {
			
			for (int z = currentLayerMinPoint.getBlockZ(); z <= iterationMaxPoint.getZ(); z++) {
				for (int x = (int) Math.ceil(currentLayerMinPoint.getX()); x <= currentLayerMaxPoint.getX(); x++) {
					for (int y = (int) Math.ceil(currentLayerMinPoint.getY()); y <= currentLayerMaxPoint.getY(); y++) {
						
						addSurroundingBlocks(x, y, z, projection, blocksInFrustum);
					}
				}
				
				currentLayerMinPoint.add(layerMinPointStep);
				currentLayerMaxPoint.add(layerMaxPointStep);
			}
			
		} else {
			
			for (int x = currentLayerMinPoint.getBlockX(); x <= iterationMaxPoint.getX(); x++) {
				for (int z = (int) Math.ceil(currentLayerMinPoint.getZ()); z <= currentLayerMaxPoint.getZ(); z++) {
					for (int y = (int) Math.ceil(currentLayerMinPoint.getY()); y <= currentLayerMaxPoint.getY(); y++) {
						
						addSurroundingBlocks(x, y, z, projection, blocksInFrustum);
					}
				}
				
				currentLayerMinPoint.add(layerMinPointStep);
				currentLayerMaxPoint.add(layerMaxPointStep);
			}
		}
		
		return blocksInFrustum;
	}
	
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
						blocksInFrustum.put(new BlockVec(x + dx, y + dy, z + dz), blockType);
					}
				}
			}
		}
	}
	
	public double getLength() {
		return frustumLength;
	}
}