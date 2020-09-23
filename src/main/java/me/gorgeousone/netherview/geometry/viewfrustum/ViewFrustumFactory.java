package me.gorgeousone.netherview.geometry.viewfrustum;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.Line;
import me.gorgeousone.netherview.geometry.Plane;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ViewFrustumFactory {
	
	private ViewFrustumFactory() {}
	
	/**
	 * Returns a viewing frustum with a near plane precisely representing the area the player can see through the portal.
	 */
	public static ViewFrustum createFrustum(Vector viewPoint, AxisAlignedRect portalRect, int frustumLength) {
		
		boolean isPlayerBehindPortal = isPlayerBehindPortal(viewPoint, portalRect);
		
		Vector portalNormal = portalRect.getPlane().getNormal();
		Vector playerFacingToPortal = portalNormal.multiply(isPlayerBehindPortal ? 1 : -1);
		
		//this will become near plane of the viewing frustum. It will be cropped to fit the actual player view through the portal
		AxisAlignedRect totalViewingRect = portalRect.clone().translate(playerFacingToPortal.clone().multiply(0.5));
		Plane portalPlane = totalViewingRect.getPlane();
		
		Vector viewingRectMin = totalViewingRect.getMin();
		Vector viewingRectMax = totalViewingRect.getMax();
		
		addTolerance(viewingRectMin, viewingRectMax, portalRect, 0.15);
		
		//depending on which portal frame blocks will block the view, the viewing rect bounds are contracted by casting rays along the block edges
		//here for the height of the rect...
		if (viewPoint.getY() < viewingRectMin.getY()) {
			
			Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
			Vector newRectMin = portalPlane.getIntersection(new Line(viewPoint, closeRectMin));
			viewingRectMin.setY(newRectMin.getY());
			
		} else if (viewPoint.getY() > viewingRectMax.getY()) {
			
			Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
			Vector newRectMax = portalPlane.getIntersection(new Line(viewPoint, closeRectMax));
			viewingRectMax.setY(newRectMax.getY());
		}
		
		Axis portalAxis = portalRect.getAxis();
		
		//... also for the width
		if (portalAxis == Axis.X) {
			
			if (viewPoint.getX() < viewingRectMin.getX()) {
				
				Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
				Vector newRectMin = portalPlane.getIntersection(new Line(viewPoint, closeRectMin));
				viewingRectMin.setX(newRectMin.getX());
				
			} else if (viewPoint.getX() > viewingRectMax.getX()) {
				
				Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
				Vector newRectMax = portalPlane.getIntersection(new Line(viewPoint, closeRectMax));
				viewingRectMax.setX(newRectMax.getX());
			}
			
		} else {
			
			if (viewPoint.getZ() < viewingRectMin.getZ()) {
				
				Vector closeRectMin = viewingRectMin.clone().subtract(playerFacingToPortal);
				Vector newRectMin = portalPlane.getIntersection(new Line(viewPoint, closeRectMin));
				viewingRectMin.setZ(newRectMin.getZ());
				
			} else if (viewPoint.getZ() > viewingRectMax.getZ()) {
				
				Vector closeRectMax = viewingRectMax.clone().subtract(playerFacingToPortal);
				Vector newRectMax = portalPlane.getIntersection(new Line(viewPoint, closeRectMax));
				viewingRectMax.setZ(newRectMax.getZ());
			}
		}
		
		try {
			AxisAlignedRect actualViewingRect = new AxisAlignedRect(totalViewingRect.getAxis(), viewingRectMin, viewingRectMax);
			return new ViewFrustum(viewPoint, actualViewingRect, frustumLength);
			
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static boolean isPlayerBehindPortal(Player player, Portal portal) {
		return isPlayerBehindPortal(player.getEyeLocation().toVector(), portal.getPortalRect());
	}
	
	public static boolean isPlayerBehindPortal(Vector viewPoint, AxisAlignedRect portalRect) {
		
		Vector portalPos = portalRect.getMin();
		
		return portalRect.getAxis() == Axis.X ?
				viewPoint.getZ() < portalPos.getZ() :
				viewPoint.getX() < portalPos.getX();
	}
	
	//widen the rectangle bounds a bit so the projection becomes smoother/more consistent when moving quickly
	//side effects are blocks slightly sticking out at the sides when standing further away
	private static void addTolerance(Vector rectMin, Vector rectMax, AxisAlignedRect portalRect, double tolerance) {
		
		Vector toleranceVec = portalRect.getCrossNormal();
		toleranceVec.setY(1);
		toleranceVec.multiply(tolerance);
		
		rectMin.subtract(toleranceVec);
		rectMax.add(toleranceVec);
	}
}