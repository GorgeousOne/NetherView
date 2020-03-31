package threedtests;

import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.threedstuff.Transform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransformationTests {
	
	@Test
	public void translatePoint() {
		
		Transform transform = new Transform();
		transform.setTranslation(new BlockVec(100, 0, 100));
		
		BlockVec point = new BlockVec(10, 20, 30);
		BlockVec transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(110, transformedPoint.getX());
		Assertions.assertEquals(20, transformedPoint.getY());
		Assertions.assertEquals(130, transformedPoint.getZ());
	}
	
	@Test
	public void rotatePoint() {
		
		Transform transform = new Transform();
		transform.setRotY90DegLeft();
		
		BlockVec point = new BlockVec(20, 0, 50);
		BlockVec transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(50, transformedPoint.getX());
		Assertions.assertEquals(0, transformedPoint.getY());
		Assertions.assertEquals(-20, transformedPoint.getZ());
	}
	
	@Test
	public void rotatePointAroundPoint() {
		
		Transform transform = new Transform();
		transform.setRotY90DegLeft();
		transform.setRotCenter(new BlockVec(50, 0, 20));
		
		BlockVec point = new BlockVec(0, 0, 0);
		BlockVec transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(30, transformedPoint.getX());
		Assertions.assertEquals(0, transformedPoint.getY());
		Assertions.assertEquals(70, transformedPoint.getZ());
	}
	
	@Test
	public void rotatePointAroundPoint2() {
		
		Transform transform = new Transform();
		transform.setRotY180Deg();
		transform.setRotCenter(new BlockVec(50, 0, 20));
		
		BlockVec point = new BlockVec(0, 0, 0);
		BlockVec transformedPoint = transform.getTransformed(point);
		
		Assertions.assertEquals(100, transformedPoint.getX());
		Assertions.assertEquals(0, transformedPoint.getY());
		Assertions.assertEquals(40, transformedPoint.getZ());
	}
	
	@Test
	public void forthBackTransform() {
		
		Transform transform = new Transform();
		transform.setTranslation(new BlockVec(100, 0, 100));
		transform.setRotY180Deg();
		
		BlockVec point = new BlockVec(50 ,0, 0);
		
		BlockVec transformedPoint = transform.getTransformed(point);
		transform.invert();
		transformedPoint = transform.getTransformed(transformedPoint);
		
		Assertions.assertEquals(point, transformedPoint);
	}
}
