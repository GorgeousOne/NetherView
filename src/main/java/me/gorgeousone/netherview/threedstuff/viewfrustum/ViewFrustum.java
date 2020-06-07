package me.gorgeousone.netherview.threedstuff.viewfrustum;

import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.Line;
import me.gorgeousone.netherview.threedstuff.Plane;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

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
	 * Iterates through the view frustum by iterating through the exact frustum shape and listing all locations contained by it.
	 * @return a Set of block locations in the frustum.
	 */
	public Set<BlockVec> getContainedBlockLocs() {
		
		AxisAlignedRect minLayer;
		AxisAlignedRect maxLayer;
		
		if (frustumFacing.getX() == -1 || frustumFacing.getZ() == -1) {
			minLayer = farPlaneRect;
			maxLayer = nearPlaneRect;
			
		} else {
			minLayer = nearPlaneRect;
			maxLayer = farPlaneRect;
		}
		
		Vector iterationMax = maxLayer.getMax();
		Vector currentLayerMax = minLayer.getMax();
		
		Vector layerMinStep = maxLayer.getMin().subtract(minLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxStep = maxLayer.getMax().subtract(minLayer.getMax()).multiply(1d / frustumLength);
	
		Vector currentLayer = minLayer.getMin();
		Vector currentColumn = currentLayer.clone();
		Vector currentLoc = currentLayer.clone();
		
		Set<BlockVec> blockLocs= new HashSet<>();
		
		while (true) {
			
			blockLocs.add(new BlockVec(currentLoc));
			
			if (currentLoc.getBlockY() + 1 <= currentLayerMax.getY()) {
				currentLoc.setY(currentLoc.getY() + 1);
				continue;
			}
			
			if (nearPlaneRect.getAxis() == Axis.X) {
				
				if (currentLoc.getBlockX() + 1 <= currentLayerMax.getX()) {
					
					currentColumn.setX(currentColumn.getX() + 1);
					currentLoc = currentColumn.clone();
					continue;
				}
				
				if (currentLoc.getBlockZ() + 1 <= iterationMax.getZ()) {
					
					currentLayer.add(layerMinStep);
					currentLayerMax.add(layerMaxStep);
					
					currentColumn = currentLayer.clone();
					currentLoc = currentLayer.clone();
					continue;
				}
				
			} else {
				
				if (currentLoc.getBlockZ() + 1 <= currentLayerMax.getZ()) {
					
					currentColumn.setZ(currentColumn.getZ() + 1);
					currentLoc = currentColumn.clone();
					continue;
				}
				
				if (currentLoc.getBlockX() + 1 <= iterationMax.getX()) {
					
					currentLayer.add(layerMinStep);
					currentLayerMax.add(layerMaxStep);
					
					currentColumn = currentLayer.clone();
					currentLoc = currentLayer.clone();
					continue;
				}
			}
			
			return blockLocs;
		}
	}
	
	public double getLength() {
		return frustumLength;
	}
}