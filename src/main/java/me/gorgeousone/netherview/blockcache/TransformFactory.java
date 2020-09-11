package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapping.Axis;

public class TransformFactory {
	
	/**
	 * Calculates a Transform that can translate and rotate the block cache blocks from one portal
	 * to the related projection cache blocks of another portal.
	 *
	 * @param portal that has the projection cache
	 * @param counterPortal that has the block cache related to it
	 * @param isViewFlipped flips the transform rotation by 180 degrees
	 */
	public static Transform calculateLinkTransform(Portal portal, Portal counterPortal, boolean isViewFlipped) {
		
		Transform linkTransform = new Transform();
		Axis counterPortalAxis = counterPortal.getAxis();
		
		BlockVec portalLoc1 = portal.getMin();
		BlockVec portalLoc2;
		
		if (portal.getAxis() == counterPortalAxis) {
			
			if (isViewFlipped) {
				portalLoc2 = counterPortal.getMin();
				
			} else {
				portalLoc2 = counterPortal.getMaxBlockAtFloor();
				linkTransform.setRotY180Deg();
			}
			
		} else {
			
			if (counterPortalAxis == Axis.X ^ isViewFlipped) {
				linkTransform.setRotY90DegRight();
			} else {
				linkTransform.setRotY90DegLeft();
			}
			
			portalLoc2 = isViewFlipped ? counterPortal.getMaxBlockAtFloor() : counterPortal.getMin();
		}
		
		linkTransform.setRotCenter(portalLoc2);
		linkTransform.setTranslation(portalLoc1.subtract(portalLoc2));
		return linkTransform;
	}
}
