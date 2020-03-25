package me.gorgeousone.netherview.threedstuff;

import org.bukkit.util.Vector;

public class ViewCone {
	
	private Vector viewPoint;
	private AxisAlignedRect nearPlane;
	
	public ViewCone(Vector viewPoint, AxisAlignedRect nearPlane) {
		this.viewPoint = viewPoint;
		this.nearPlane = nearPlane;
	}
	
	public boolean contains(Vector point) {
	
		Line viewRay = new Line(viewPoint, point);
		Vector pointInNearPlane = nearPlane.getPlane().getIntersection(viewRay);
		
		return nearPlane.rectContains(pointInNearPlane);
	}
}
