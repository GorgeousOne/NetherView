package threedtests;

import me.gorgeousone.netherview.threedstuff.Rectangle;
import me.gorgeousone.netherview.threedstuff.Transform;
import me.gorgeousone.netherview.threedstuff.ViewCone;
import org.bukkit.Axis;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreeDTests {

	@Test
	public void pointInRect() {
		
		Rectangle rect = new Rectangle(Axis.X, new Vector(), 5, 10);
		
		Vector containedPoint = new Vector(2, 7, 0);
		Vector notContainedPoint = new Vector( -2, 7, 0);
		
		Assertions.assertTrue(rect.contains(containedPoint));
		Assertions.assertFalse(rect.contains(notContainedPoint));
	}
	
	@Test
	public void pointInCone() {

		Rectangle nearPlane = new Rectangle(Axis.X, new Vector(), 5, 10);
		Vector viewPoint = new Vector(2.5, 5, -2.5);
		ViewCone cone = new ViewCone(viewPoint, nearPlane);
		
		Vector containedPoint = new Vector(2.5, 5, 1);
		Vector notContainedPoint = new Vector(-10, 5, 1);
		Vector pointBeyondNearPlane = new Vector(2.5, 5, -1);
		Vector almostContainedPoint = new Vector(-2.5001, 5, 2.5);
		Vector almostNotContainedPoint = new Vector(-2.4999, 5, 2.5);
		
		Assertions.assertTrue(cone.contains(containedPoint));
		Assertions.assertFalse(cone.contains(notContainedPoint));
		Assertions.assertFalse(cone.contains(pointBeyondNearPlane));
		Assertions.assertFalse(cone.contains(almostContainedPoint));
		Assertions.assertTrue(cone.contains(almostNotContainedPoint));
	}
	
	@Test
	public void translatePoint() {
		
		Transform transform = new Transform();
		transform.setTranslation(new Vector(100, 0, 100));
		
		Vector point = new Vector(10, 20, 30);
		Vector transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(110, transformedPoint.getX());
		Assertions.assertEquals(20, transformedPoint.getY());
		Assertions.assertEquals(130, transformedPoint.getZ());
	}
	
	@Test
	public void rotatePoint() {
		
		Transform transform = new Transform();
		transform.setRotY90DegLeft();
		
		Vector point = new Vector(20, 0, 50);
		Vector transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(50, transformedPoint.getX());
		Assertions.assertEquals(0, transformedPoint.getY());
		Assertions.assertEquals(-20, transformedPoint.getZ());
	}
	
	@Test
	public void rotatePointAroundPoint() {
		
		Transform transform = new Transform();
		transform.setRotY90DegLeft();
		transform.setRotCenter(new Vector(50, 0, 20));
		
		Vector point = new Vector(0, 0, 0);
		Vector transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(30, transformedPoint.getX());
		Assertions.assertEquals(0, transformedPoint.getY());
		Assertions.assertEquals(70, transformedPoint.getZ());
	}
	
	@Test
	public void forthBackTransform() {
		
		Transform transform = new Transform();
		transform.setTranslation(new Vector(100, 0, 100));
		transform.setRotY90DegLeft();
		
		Vector point = new Vector(50 ,0, 0);
		
		Vector transformedPoint = transform.getTransformed(point);
		transform.invert();
		transformedPoint = transform.getTransformed(transformedPoint);
		
		Assertions.assertEquals(point, transformedPoint);
	}
}