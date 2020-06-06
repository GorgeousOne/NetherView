package threedtests;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.viewfrustum.ViewFrustum;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ViewFrustumTests {

	@Test
	public void pointInRect() {
		
		AxisAlignedRect rect = new AxisAlignedRect(Axis.X, new Vector(), 5, 10);
		
		Vector containedPoint = new Vector(2, 7, 0);
		Vector notContainedPoint = new Vector( -2, 7, 0);
		
		Assertions.assertTrue(rect.contains(containedPoint));
		Assertions.assertFalse(rect.contains(notContainedPoint));
	}
	
	@Test
	public void allBlockLocsInFrustum() {
		
		AxisAlignedRect nearPlane = new AxisAlignedRect(Axis.X, new Vector(-1, -1, 0), 2, 2);
		ViewFrustum frustum = new ViewFrustum(new Vector(0, 0, -1), nearPlane, 2);
		
		Set<BlockVec> locsIteratedManually = new HashSet<>();
		
		for (int x = -10; x <= 10; x++) {
			for (int y = -10; y <= 10; y++) {
				for (int z = -1; z <= frustum.getLength(); z++) {
					
					BlockVec blockLoc = new BlockVec(x, y, z);
					
					if (frustum.contains(blockLoc.toVector())) {
						locsIteratedManually.add(blockLoc);
						
						if(y == 0)
							System.out.println(blockLoc);
					}
				}
			}
		}
		
		Set<BlockVec> locsCreatedEfficiently = frustum.getContainedBlockLocs();
		
		System.out.println(locsCreatedEfficiently.size() + " auto");
	}
	
//	@Test
//	public void pointInCone() {
//
//		AxisAlignedRect nearPlane = new AxisAlignedRect(Axis.X, new Vector(100, 0, 0), 5, 10);
//		Vector viewPoint = new Vector(2.5, 5, -2.5);
//		ViewingFrustum cone = new ViewingFrustum(viewPoint, nearPlane);
//
//		Vector containedPoint = new Vector(2.5, 5, 1);
//		Vector notContainedPoint = new Vector(-10, 5, 1);
//		Vector pointBeyondNearPlane = new Vector(2.5, 5, -1);
//		Vector almostContainedPoint = new Vector(-2.5001, 5, 2.5);
//		Vector almostNotContainedPoint = new Vector(-2.4999, 5, 2.5);
//
//		Assertions.assertTrue(cone.contains(containedPoint));
//		Assertions.assertFalse(cone.contains(notContainedPoint));
//		Assertions.assertFalse(cone.contains(pointBeyondNearPlane));
//		Assertions.assertFalse(cone.contains(almostContainedPoint));
//		Assertions.assertTrue(cone.contains(almostNotContainedPoint));
//	}
//
//	@Test
//	public void example() {
//
//		Vector viewPoint = new Vector(22, 67, 35);
//
//		AxisAlignedRect nearPlane = new AxisAlignedRect(Axis.Z, new Vector(27, 66, 34), 2, 4);
//		ViewingFrustum cone = new ViewingFrustum(viewPoint, nearPlane);
//		Vector point = new Vector(28, 68, 35);
//
//		DefinedLine lineOfView = new DefinedLine(viewPoint, point);
//		Vector pointInNearPlane = nearPlane.getPlane().getIntersection(lineOfView);
//
//		Assertions.assertNotEquals(null, pointInNearPlane);
//		System.out.println("This is the intersection point " + pointInNearPlane.toString());
//
//		Assertions.assertTrue(nearPlane.contains(pointInNearPlane));
//		Assertions.assertEquals(new Vector(27, 70, 36), nearPlane.getMax());
//		Assertions.assertTrue(cone.contains(point));
//	}
}