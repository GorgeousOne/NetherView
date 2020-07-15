package me.gorgeousone.netherview.geometry.viewfrustum;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.Line;
import me.gorgeousone.netherview.geometry.Plane;
import org.bukkit.util.Vector;

/**
 * A viewing frustum for detecting the blocks of a projection cache that can be seen through a portal frame.
 * It is a frustum with the specific condition that near and far plane are rectangles either aligned to the x or z axis.
 */
public class ViewFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlaneRect;
	private AxisAlignedRect farPlaneRect;
	
	private int length;
	private Vector facing;
	
	public ViewFrustum(Vector viewPoint, AxisAlignedRect nearPlane, int length) {
		
		this.viewPoint = viewPoint;
		this.nearPlaneRect = nearPlane;
		this.length = length;
		
		createFrustumFacing();
		createFarPlaneRect();
	}
	
	public AxisAlignedRect getNearPlaneRect() {
		return nearPlaneRect;
	}
	
	public AxisAlignedRect getFarPlaneRect() {
		return farPlaneRect;
	}
	
	public Vector getFacing() {
		return facing;
	}
	
	public int getLength() {
		return length;
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
		this.facing = nearPlaneRect.getNormal();
		
		//but make it face in the opposite direction of where the view point
		Vector relViewPoint = viewPoint.clone().subtract(nearPlaneRect.getMin());
		facing.multiply(-Math.signum(facing.dot(relViewPoint)));
	}
	
	private void createFarPlaneRect() {
		
		Vector nearPlaneOrigin = nearPlaneRect.getMin();
		Vector nearPlaneNormal = nearPlaneRect.getNormal();
		
		//calculate a vector representing the distance between near plane and far plane
		Vector farPlaneOffset = facing.clone().multiply(length);
		
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
}