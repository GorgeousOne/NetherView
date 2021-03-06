package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageException;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class CustomPortalCreator {
	
	public static CustomPortal createPortal(World world, Cuboid portalFrame) throws MessageException {
		
		int widthX = portalFrame.getWidthX();
		int widthZ = portalFrame.getWidthZ();
		
		if (widthX > 1 && widthZ > 1) {
			throw new MessageException(Message.SELECTION_NOT_FLAT);
		}
		
		Axis axis = widthX == 1 ? Axis.Z : Axis.X;
		BlockVec frameExtent = new BlockVec(axis.getCrossNormal()).setY(1);
		
		Cuboid portalInner = portalFrame.clone()
				.translateMin(frameExtent)
				.translateMax(frameExtent.multiply(-1));
		
		Vector rectMin = portalInner.getMin().toVector();
		Vector rectMax = portalInner.getMax().toVector().subtract(axis.getNormal());
		AxisAlignedRect portalRect;
		
		try {
			portalRect = new AxisAlignedRect(axis, rectMin, rectMax);
			portalRect.translate(axis.getNormal().multiply(0.5));
			
		} catch (IllegalArgumentException e) {
			throw new MessageException(Message.SELECTION_TOO_SMALL);
		}
		
		if (portalRect.width() < 1 || portalRect.height() < 1) {
			throw new MessageException(Message.SELECTION_TOO_SMALL);
		} else if (portalRect.width() > 20 || portalRect.height() > 20) {
			throw new MessageException(Message.PORTAL_TOO_BIG, "20");
		}
		
		return new CustomPortal(world, portalRect, portalFrame, portalInner);
	}
}
