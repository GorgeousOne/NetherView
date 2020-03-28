package threedtests;

import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransformationTests {
	
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
