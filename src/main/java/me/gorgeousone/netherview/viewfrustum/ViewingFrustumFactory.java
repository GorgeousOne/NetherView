package me.gorgeousone.netherview.viewfrustum;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.Line;
import org.bukkit.Axis;
import org.bukkit.util.Vector;

public final class ViewingFrustumFactory {
	
	private ViewingFrustumFactory() {}
	
	/**
	 * Returns a viewing frustum with a near plane roughly representing the area the player can see through the portal.
	 */
	public static ViewingFrustum createFrustum(Vector viewPoint, AxisAlignedRect portalRect) {
		
		boolean isPlayerBehindPortal = isPlayerBehindPortal(viewPoint, portalRect);
		Vector portalNormal = portalRect.getPlane().getNormal();
		Vector playerFacingToPortal = portalNormal.clone().multiply(isPlayerBehindPortal ? 1 : -1);
		
		//clone the portalRect and simply translate it towards the side of the portal which is further away from the player
		AxisAlignedRect nearPlane = portalRect.clone();
		nearPlane.translate(playerFacingToPortal.multiply(0.5));
		
		return new ViewingFrustum(viewPoint, nearPlane);
	}
	
	public static ViewingFrustum createFrustum2(Vector viewPoint, AxisAlignedRect portalRect) {

		boolean isPlayerBehindPortal = isPlayerBehindPortal(viewPoint, portalRect);

		Vector portalNormal = portalRect.getPlane().getNormal();
		Vector playerFacingToPortal = portalNormal.clone().multiply(isPlayerBehindPortal ? 1 : -1);

		//aka near plane of the viewing frustum. the maximum rectangle the player can possibly see
		AxisAlignedRect maxViewingRect = portalRect.clone().translate(playerFacingToPortal.clone().multiply(0.5));
		
		Vector viewingRectMin = maxViewingRect.getMin();
		Vector viewingRectMax = maxViewingRect.getMax();

		//if needed the viewing rect bounds are contracted with ray casting
		//here for the height...
		if(viewPoint.getY() < viewingRectMin.getY()) {
			
			Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
			Vector newRectMin = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMin));
			viewingRectMin.setY(newRectMin.getY());
			
		}else if(viewPoint.getY() > viewingRectMax.getY()) {
			
			Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
			Vector newRectMax = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMax));
			viewingRectMax.setY(newRectMax.getY());
		}

		Axis portalAxis = portalRect.getAxis();
		
		//... also for the width
		if(portalAxis == Axis.X) {

			if(viewPoint.getX() < viewingRectMin.getX()) {
				
				Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
				Vector newRectMin = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMin));
				viewingRectMin.setX(newRectMin.getX());
				
			}else if(viewPoint.getX() > viewingRectMax.getX()) {
				
				Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
				Vector newRectMax = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMax));
				viewingRectMax.setX(newRectMax.getX());
			}

		}else {

			if(viewPoint.getZ() < viewingRectMin.getZ()) {
				
				Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
				Vector newRectMin = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMin));
				viewingRectMin.setZ(newRectMin.getZ());
				
			}else if(viewPoint.getZ() > viewingRectMax.getZ()) {
				
				Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
				Vector newRectMax = maxViewingRect.getPlane().getIntersection(new Line(viewPoint, closeRectMax));
				viewingRectMax.setZ(newRectMax.getZ());
			}
		}

		Vector viewingRectSize = viewingRectMax.clone().subtract(viewingRectMin);
		double rectWidth = portalAxis == Axis.X ? viewingRectSize.getX() : viewingRectSize.getZ();
		double rectHeight = viewingRectSize.getY();
		
		//the new contracted rect that is left visible
		AxisAlignedRect actualViewingRect = new AxisAlignedRect(maxViewingRect.getAxis(), viewingRectMin, rectWidth, rectHeight);
		
		return new ViewingFrustum(viewPoint, actualViewingRect);
	}
	
	private static boolean isPlayerBehindPortal(Vector viewPoint, AxisAlignedRect portalRect) {
		
		Vector portalPos = portalRect.getMin();
		
		return portalRect.getAxis() == Axis.X ?
				viewPoint.getZ() < portalPos.getZ() :
				viewPoint.getX() < portalPos.getX();
	}
}
