package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalType;
import me.gorgeousone.netherview.utils.MessageException;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class PortalCreator {
	
	public static Portal createPortal(World world, Cuboid frameShape) throws MessageException {
		
		int widthX = frameShape.getWidthX();
		int widthZ = frameShape.getWidthZ();
		
		if (widthX > 1 && widthZ > 1) {
			throw new MessageException(Message.SELECTION_NOT_FLAT);
		}
		
		Axis axis = widthX == 1 ? Axis.Z : Axis.X;
		BlockVec frameExtent = new BlockVec(axis.getCrossNormal()).setY(1);
		
		Cuboid innerShape = frameShape.clone()
				.translateMin(frameExtent)
				.translateMax(frameExtent.multiply(-1));
		
		Vector rectMin = innerShape.getMin().toVector();
		Vector rectMax = innerShape.getMax().toVector().subtract(axis.getNormal());
		AxisAlignedRect portalRect;
		
		try {
			portalRect = new AxisAlignedRect(axis, rectMin, rectMax);
			portalRect.translate(axis.getNormal().multiply(0.5));
		
		}catch (IllegalArgumentException e) {
			throw new MessageException(Message.SELECTION_TOO_SMALL);
		}
		
		if (portalRect.width() < 1 || portalRect.height() < 1) {
			throw new MessageException(Message.SELECTION_TOO_SMALL);
		}else if (portalRect.width() > 20 || portalRect.height() > 20) {
			throw new MessageException(Message.PORTAL_TOO_BIG, "20");
		}
		
		return new Portal(world, portalRect, new HashSet<>(), frameShape, innerShape, PortalType.CUSTOM);
	}
}
