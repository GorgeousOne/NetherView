package me.gorgeousone.netherview.threedstuff.viewfrustum;

import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.Line;
import me.gorgeousone.netherview.threedstuff.Plane;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	 *
	 * @return a Set of block locations in the frustum.
	 */
	public Map<BlockVec, BlockType> getContainedBlockLocs(ProjectionCache cache) {
		
		long start0 = System.currentTimeMillis();
		
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
		Vector currentLayerMin = minLayer.getMin();
		Vector currentLayerMax = minLayer.getMax();
		
		Vector layerMinStep = maxLayer.getMin().subtract(minLayer.getMin()).multiply(1d / frustumLength);
		Vector layerMaxStep = maxLayer.getMax().subtract(minLayer.getMax()).multiply(1d / frustumLength);
		
		Map<BlockVec, BlockType> blockLocs = new HashMap<>();
		
		if (nearPlaneRect.getAxis() == Axis.X) {
			
			long start = System.currentTimeMillis();
			
			for (int z = currentLayerMin.getBlockZ(); z < iterationMax.getZ(); z++) {
				for (int x = (int) Math.ceil(currentLayerMin.getX()); x < currentLayerMax.getX(); x++) {
					for (int y = (int) Math.ceil(currentLayerMin.getY()); y < currentLayerMax.getY(); y++) {
						
						blockLocs.putAll(cache.getBlocksAroundPositiveZ(x, y, z));
					}
				}
				
				currentLayerMin.add(layerMinStep);
				currentLayerMax.add(layerMaxStep);
			}
			System.out.println("auto time: " + (System.currentTimeMillis() - start) + " - found blocks: " + blockLocs.size() + " prep time: " + (start - start0));
			
		} else {
			
			long start = System.currentTimeMillis();
			
			for (int x = currentLayerMin.getBlockX(); x < iterationMax.getX(); x++) {
				for (int z = (int) Math.ceil(currentLayerMin.getZ()); z < currentLayerMax.getZ(); z++) {
					for (int y = (int) Math.ceil(currentLayerMin.getY()); y < currentLayerMax.getY(); y++) {
					
						blockLocs.putAll(cache.getBlocksAroundPositiveX(x, y, z));
					}
				}
				
				currentLayerMin.add(layerMinStep);
				currentLayerMax.add(layerMaxStep);
			}
			System.out.println("auto time: " + (System.currentTimeMillis() - start) + " - found blocks: " + blockLocs.size());
			
		}
		
		return blockLocs;
	}
	
	public double getLength() {
		return frustumLength;
	}
}