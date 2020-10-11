package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalType;
import me.gorgeousone.netherview.utils.MessageException;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class PortalCreator {
	
	public static Portal createPortal(World world, Cuboid cuboid) throws MessageException {
		
		int widthX = cuboid.getWidthX();
		int widthZ = cuboid.getWidthZ();
		
		if (widthX > 1 && widthZ > 1) {
			throw new MessageException(Message.SELECTION_NOT_FLAT);
		}
		
		Axis axis = widthX == 1 ? Axis.Z : Axis.X;
		Vector frameExtent = axis.getCrossNormal();
		frameExtent.setY(1);
		
		Vector rectMin = cuboid.getMinBlock().toVector().add(frameExtent);
		Vector rectMax = cuboid.getMaxBlock().toVector();
		AxisAlignedRect portalRect;
		
		try {
			portalRect = new AxisAlignedRect(axis, rectMin, rectMax);
			portalRect.translate(axis.getNormal().multiply(0.5));
		
		}catch (IllegalArgumentException e) {
			throw new MessageException(Message.PORTAL_TOO_BIG, "3");
		}
		
		
		if (portalRect.width() < 1 || portalRect.height() < 1) {
			throw new MessageException(Message.PORTAL_TOO_BIG, "" + portalRect.width());
		}else if (portalRect.width() > 20 || portalRect.height() > 20) {
			throw new MessageException(Message.PORTAL_TOO_BIG, "20");
		}
		
		return new Portal(world, portalRect, new HashSet<>(), cuboid.getMinBlock(), cuboid.getMaxBlock(), PortalType.CUSTOM);
	}
}
