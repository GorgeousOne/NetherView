package threedtests;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.blocktype.Axis;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ViewingFrustumTests {

	@Test
	public void pointInRect() {
		
		AxisAlignedRect rect = new AxisAlignedRect(Axis.X, new Vector(), 5, 10);
		
		Vector containedPoint = new Vector(2, 7, 0);
		Vector notContainedPoint = new Vector( -2, 7, 0);
		
		Assertions.assertTrue(rect.contains(containedPoint));
		Assertions.assertFalse(rect.contains(notContainedPoint));
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