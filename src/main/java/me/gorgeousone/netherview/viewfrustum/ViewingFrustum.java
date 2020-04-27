package me.gorgeousone.netherview.viewfrustum;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.util.Vector;

public class ViewingFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlane;
	
	public ViewingFrustum(Vector viewPoint, AxisAlignedRect nearPlane) {
		this.viewPoint = viewPoint;
		this.nearPlane = nearPlane;
	}
	
	public AxisAlignedRect getNearPlane() {
		return nearPlane;
	}
	
	public boolean contains(Vector point) {
		
		DefinedLine lineOfView = new DefinedLine(viewPoint, point);
		Vector pointInNearPlane = nearPlane.getPlane().getIntersection(lineOfView);
		
		return pointInNearPlane != null && nearPlane.contains(pointInNearPlane);
	}
}