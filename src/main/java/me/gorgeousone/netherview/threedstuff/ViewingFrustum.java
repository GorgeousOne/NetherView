package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class ViewingFrustum {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlane;
	
	public ViewingFrustum(Vector viewPoint, AxisAlignedRect nearPlane) {
		this.viewPoint = viewPoint;
		this.nearPlane = nearPlane;
	}
	
	public boolean contains(Vector point) {
		
		DefinedLine lineOfView = new DefinedLine(viewPoint, point);
		Vector pointInNearPlane = nearPlane.getPlane().getIntersection(lineOfView);
		
		return pointInNearPlane != null && nearPlane.contains(pointInNearPlane);
	}
}