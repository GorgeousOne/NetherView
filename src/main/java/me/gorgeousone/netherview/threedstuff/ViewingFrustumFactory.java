package me.gorgeousone.netherview.threedstuff;

import me.gorgeousone.netherview.portal.PortalStructure;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ViewingFrustumFactory {
	
	public static ViewingFrustum createViewingFrustum(Location playerLoc, PortalStructure portal) {
		
		AxisAlignedRect nearPlane = portal.getPortalRect();
		Vector portalNormal = nearPlane.getPlane().getNormal();
		
		nearPlane.translate(portalNormal.clone().multiply(0.5));
		
		boolean isPlayerInNegative = isPlayerRelativelyNegativeToPortal(playerLoc, portal);
		Vector generalPlayerFacing = portalNormal.clone().multiply(isPlayerInNegative ? 0.5 : -0.5);
		
		Vector minCorner1 = nearPlane.getMin().add(generalPlayerFacing.clone().multiply(-1));
		Vector minCorner2 = nearPlane.getMin().add(generalPlayerFacing);
		
		Vector maxCorner1 = nearPlane.getMax().add(generalPlayerFacing.clone().multiply(-1));
		Vector maxCorner2 = nearPlane.getMax().add(generalPlayerFacing);
		
		Vector viewPoint = playerLoc.toVector();
		
		
		
		Vector rectMin = new Vector();
		double rectWidth;
		double rectHeight;
		
		Vector minFramingEdgeY;
		Vector maxFramingEdgeY;
		
		
		if (isPlayerInNegative) {
			minFramingEdgeY = viewPoint.getY() > minCorner1.getY() ? minCorner1 : minCorner2;
			maxFramingEdgeY = viewPoint.getY() < maxCorner1.getY() ? maxCorner1 : maxCorner2;
		}else {
			minFramingEdgeY = viewPoint.getY() > minCorner1.getY() ? minCorner2 : minCorner1;
			maxFramingEdgeY = viewPoint.getY() < maxCorner1.getY() ? maxCorner2 : maxCorner1;
		}
		
		Vector intersBottom = nearPlane.getPlane().getIntersection(new Line(viewPoint, minFramingEdgeY));
		Vector intersTop = nearPlane.getPlane().getIntersection(new Line(viewPoint, maxFramingEdgeY));
		World world = playerLoc.getWorld();
		world.spawnParticle(Particle.DRIP_LAVA, intersBottom.toLocation(world), 0, 0, 0, 0);
		world.spawnParticle(Particle.DRIP_LAVA, intersTop.toLocation(world), 0, 0, 0, 0);
		
		double minFrameY = nearPlane.getPlane().getIntersection(new Line(viewPoint, minFramingEdgeY)).getY();
		double maxFrameY = nearPlane.getPlane().getIntersection(new Line(viewPoint, maxFramingEdgeY)).getY();
		
		rectMin.setY(minFrameY);
		rectHeight = maxFrameY - minFrameY;
		
		if(nearPlane.getAxis() == Axis.X) {
			
			Vector minFramingEdge;
			Vector maxFramingEdge;
			
			if (isPlayerInNegative) {
				minFramingEdge = viewPoint.getX() > minCorner1.getX() ? minCorner1 : minCorner2;
				maxFramingEdge = viewPoint.getX() < maxCorner1.getX() ? maxCorner1 : maxCorner2;
			}else {
				minFramingEdge = viewPoint.getX() > minCorner1.getX() ? minCorner2 : minCorner1;
				maxFramingEdge = viewPoint.getX() < maxCorner1.getX() ? maxCorner2 : maxCorner1;
			}
			
//			Vector intersBottom = nearPlane.getPlane().getIntersection(new Line(viewPoint, minFramingEdge));
//			Vector intersTop = nearPlane.getPlane().getIntersection(new Line(viewPoint, maxFramingEdge));
//			World world = playerLoc.getWorld();
//			world.spawnParticle(Particle.DRIP_LAVA, intersBottom.toLocation(world), 0, 0, 0, 0);
//			world.spawnParticle(Particle.DRIP_LAVA, intersTop.toLocation(world), 0, 0, 0, 0);
			
			double minFrameX = nearPlane.getPlane().getIntersection(new Line(viewPoint, minFramingEdge)).getX();
			double maxFrameX = nearPlane.getPlane().getIntersection(new Line(viewPoint, maxFramingEdge)).getX();
			
			rectMin.setX(minFrameX);
			rectMin.setZ(nearPlane.getMin().getZ());
			rectWidth = maxFrameX - minFrameX;
			
		}else {
			
			Vector minFramingEdge;
			Vector maxFramingEdge;
			
			if (isPlayerInNegative) {
				minFramingEdge = viewPoint.getZ() > minCorner1.getZ() ? minCorner1 : minCorner2;
				maxFramingEdge = viewPoint.getZ() < maxCorner1.getZ() ? maxCorner1 : maxCorner2;
			}else {
				minFramingEdge = viewPoint.getZ() > minCorner1.getZ() ? minCorner2 : minCorner1;
				maxFramingEdge = viewPoint.getZ() < maxCorner1.getZ() ? maxCorner2 : maxCorner1;
			}
			
			double minFrameZ = nearPlane.getPlane().getIntersection(new Line(viewPoint, minFramingEdge)).getZ();
			double maxFrameZ = nearPlane.getPlane().getIntersection(new Line(viewPoint, maxFramingEdge)).getZ();
			
			rectMin.setZ(minFrameZ);
			rectMin.setX(nearPlane.getMin().getX());
			rectWidth = maxFrameZ - minFrameZ;
		}
		
//		World world = playerLoc.getWorld();
//		world.spawnParticle(Particle.DRIP_LAVA, rectMin.toLocation(world), 0, 0, 0, 0);
//		world.spawnParticle(Particle.DRIP_LAVA, rectMin.toLocation(world).add(rectWidth, rectHeight, 0), 0, 0, 0, 0);
		
		AxisAlignedRect viewingRect = new AxisAlignedRect(nearPlane.getAxis(), rectMin, rectWidth, rectHeight);
		return new ViewingFrustum(viewPoint, viewingRect);
	}
	
	private static boolean isPlayerRelativelyNegativeToPortal(Location playerLoc, PortalStructure portal) {
		
		Vector portalDist = portal.getLocation().toVector().subtract(playerLoc.toVector());
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		
		return portalFacing.dot(portalDist) > 0;
	}
}
