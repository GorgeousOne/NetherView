package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapper.Axis;

public class TransformFactory {
	
	/**
	 * Calculates a Transform that can translate and rotate the block cache blocks from one portal
	 * to the related projection cache blocks of another portal.
	 *
	 * @param portal          that has the projection cache
	 * @param counterPortal   that has the block cache related to it
	 * @param isPortalFlipped flips the transform rotation by 180 degrees
	 */
	public static Transform calculateBlockLinkTransform(Portal portal, Portal counterPortal, boolean isPortalFlipped) {
		
		Transform linkTransform = new Transform();
		Axis counterPortalAxis = counterPortal.getAxis();
		
		BlockVec toLoc = portal.getFrame().getMin();
		BlockVec fromLoc;
		
		if (portal.getAxis() == counterPortalAxis) {
			
			if (isPortalFlipped) {
				fromLoc = counterPortal.getMaxBlockAtFloor();
				linkTransform.setRotY180Deg();
				
			} else {
				fromLoc = counterPortal.getFrame().getMin();
			}
			
		} else {
			
			if (counterPortalAxis == Axis.X ^ isPortalFlipped) {
				linkTransform.setRotY90DegLeft();
			} else {
				linkTransform.setRotY90DegRight();
			}
			
			fromLoc = isPortalFlipped ? counterPortal.getFrame().getMin() : counterPortal.getMaxBlockAtFloor();
		}
		
		linkTransform.setRotCenter(fromLoc);
		linkTransform.setTranslation(toLoc.subtract(fromLoc));
		return linkTransform;
	}
}
