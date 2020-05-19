package me.gorgeousone.netherview.viewfrustum;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.Line;
import me.gorgeousone.netherview.threedstuff.Plane;
import org.bukkit.util.Vector;

public class ViewingFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlaneRect;
	private AxisAlignedRect farPlaneRect;
	
	public ViewingFrustum(Vector viewPoint, AxisAlignedRect nearPlane, double frustumLength) {
		
		this.viewPoint = viewPoint;
		this.nearPlaneRect = nearPlane;
		
		createFarPlaneRect(frustumLength);
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
					
					if (contains(blockPos.clone().add(new Vector(dy, dy, dz))))
						return true;
				}
			}
		}
		
		return false;
	}
	
	private void createFarPlaneRect(double frustumLength) {
		
		Vector nearPlaneOrigin = nearPlaneRect.getMin();
		Vector nearPlaneNormal = nearPlaneRect.getNormal();
		Vector viewPointFacing = viewPoint.clone().subtract(nearPlaneOrigin);
		
		//calculate a vector representing the distance between near plane and far plane
		Vector farPlaneOffset = nearPlaneNormal.clone().multiply(frustumLength);
		//make the vector face in the direction where the view point is not
		farPlaneOffset.multiply(-Math.signum(nearPlaneNormal.dot(viewPointFacing)));
		
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
}