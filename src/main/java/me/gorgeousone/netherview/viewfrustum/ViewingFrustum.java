package me.gorgeousone.netherview.viewfrustum;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.util.Vector;

public class ViewingFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlaneRect;
	private AxisAlignedRect farPlaneRect;
	
	public ViewingFrustum(Vector viewPoint, AxisAlignedRect nearPlane) {
		this.viewPoint = viewPoint;
		this.nearPlaneRect = nearPlane;
	}
	
	private void createFarPlaneRect(double frustumLength) {
	}
	
	
	public AxisAlignedRect getNearPlaneRect() {
		return nearPlaneRect;
	}
	
	public boolean contains(Vector point) {
		
		DefinedLine lineOfView = new DefinedLine(viewPoint, point);
		Vector pointInNearPlane = nearPlaneRect.getPlane().getIntersection(lineOfView);
		
		return pointInNearPlane != null && nearPlaneRect.contains(pointInNearPlane);
	}
}