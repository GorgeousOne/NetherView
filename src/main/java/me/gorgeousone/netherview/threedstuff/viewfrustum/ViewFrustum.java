package me.gorgeousone.netherview.threedstuff.viewfrustum;

import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.Line;
import me.gorgeousone.netherview.threedstuff.Plane;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ViewFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlaneRect;
	private AxisAlignedRect farPlaneRect;
	
	private double frustumLength;
	private Vector frustumFacing;
	
	public ViewFrustum(Vector viewPoint, AxisAlignedRect nearPlane, double frustumLength) {
		
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
		System.out.println("frustum facing: " + frustumFacing.toString());
	}
	
	private void createFarPlaneRect() {
		
		Vector nearPlaneOrigin = nearPlaneRect.getMin();
		Vector nearPlaneNormal = nearPlaneRect.getNormal();
		
		//calculate a vector representing the distance between near plane and far plane
		Vector farPlaneOffset = frustumFacing.clone().multiply(frustumLength);
		
		//calculate a far plane parallel to the near plane
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
	 * Iterates through the view frustum by dividing it into layers and filtering out all block locations contained by it.
	 * @return a Set of block locations in the frustum.
	 */
	public Set<BlockVec> getContainedBlockLocs() {
		
		//minimum and maximum layer of the frustum based on if the frustum is facing towards negative-whatever-direction or not
		AxisAlignedRect minLayer;
		AxisAlignedRect maxLayer;
		
		if (frustumFacing.getX() == -1 || frustumFacing.getZ() == -1) {
			minLayer = farPlaneRect;
			maxLayer = nearPlaneRect;
			
		} else {
			minLayer = nearPlaneRect;
			maxLayer = farPlaneRect;
		}
		
		//maximum point which is the end of the whole iteration
		Vector iterationMax = maxLayer.getMax();
		//maximum point where the iteration though one layer is ending
		Vector currentLayerMax = minLayer.getMax();
		
		//minimum point of the currently inspected layer
		Vector currentLayer = minLayer.getMin();
		//minimum point of the current column of the columns each layer is divided into
		Vector currentColumn = currentLayer.clone();
		//actual point the iteration is at, starting from the minimum point of the first layer
		Vector currentLoc = currentLayer.clone();
		
		//distance vector to get from the minimum of one layer to the minimum of the next layer
		Vector layerMinStep = maxLayer.getMin().subtract(minLayer.getMin()).multiply(1 / frustumLength);
		//distance vector to get from the max of one layer to the max of the next layer
		Vector layerMaxStep = maxLayer.getMax().subtract(minLayer.getMax()).multiply(1 / frustumLength);
		
		Set<BlockVec> blockLocs= new HashSet<>();
		
		while (true) {
			
			blockLocs.add(new BlockVec(currentLoc));
			
			if (currentLoc.getBlockY() + 1 <= currentLayerMax.getY()) {
				currentLoc.add(new Vector(0, 1, 0));
				continue;
			}
			
			if (nearPlaneRect.getAxis() == Axis.X) {
				
				if (currentLoc.getBlockX() + 1 <= currentLayerMax.getX()) {
					currentLoc = currentColumn.add(new Vector(1, 0, 0)).clone();
					continue;
				}
				
				if (currentLoc.getBlockZ() + 1 <= iterationMax.getZ()) {
					currentLoc = currentLayer.add(layerMinStep).clone();
					currentLayerMax.add(layerMaxStep);
					continue;
				}
				
			} else {
				
				if (currentLoc.getBlockZ() + 1 <= currentLayerMax.getZ()) {
					currentLoc = currentColumn.add(new Vector(0, 0, 1)).clone();
					continue;
				}
				
				if (currentLoc.getBlockX() + 1 <= iterationMax.getX()) {
					currentLoc = currentLayer.add(layerMinStep).clone();
					currentLayerMax.add(layerMaxStep);
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