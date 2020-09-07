package me.gorgeousone.netherview.wrapping;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.utils.NmsUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper for the extent of a bounding boxes of an entity throughout different Minecraft versions.
 */
public class WrappedBoundingBox {
	
	private static Method ENTITY_GET_AABB;
	private static Field AABB_MIN_X;
	private static Field AABB_MIN_Y;
	private static Field AABB_MAX_X;
	private static Field AABB_MAX_Y;
	
	static {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				ENTITY_GET_AABB = NmsUtils.getNmsClass("Entity").getDeclaredMethod("getBoundingBox");
				Class<?> aabbClass = NmsUtils.getNmsClass("AxisAlignedBB");
				
				AABB_MIN_X = aabbClass.getDeclaredField("a");
				AABB_MIN_Y = aabbClass.getDeclaredField("b");
				AABB_MAX_X = aabbClass.getDeclaredField("d");
				AABB_MAX_Y = aabbClass.getDeclaredField("e");
				
			} catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final List<Vector> vertices;
	
	public WrappedBoundingBox(Entity entity, double width, double height) {
		
		Vector min = entity.getLocation().subtract(
				width / 2,
				0,
				width / 2).toVector();
		
		Vector max = entity.getLocation().add(
				width / 2,
				height,
				width / 2).toVector();
		
		vertices = new ArrayList<>(Arrays.asList(
				min,
				new Vector(max.getX(), min.getY(), min.getZ()),
				new Vector(min.getX(), min.getY(), max.getZ()),
				new Vector(max.getX(), min.getY(), max.getZ()),
				new Vector(min.getX(), max.getY(), min.getZ()),
				new Vector(max.getX(), max.getY(), min.getZ()),
				new Vector(min.getX(), max.getY(), max.getZ()),
				max
		));
	}
	
	/**
	 * Returns the 8 vertices of the entity's bounding box
	 */
	public List<Vector> getVertices() {
		return vertices;
	}
	
	public static WrappedBoundingBox of(Entity entity) {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				Object entityAabb = ENTITY_GET_AABB.invoke(NmsUtils.getHandle(entity));
				
				return new WrappedBoundingBox(
						entity,
						getBoxWidth(entityAabb),
						getBoxHeight(entityAabb));
				
			} catch (IllegalAccessException | InvocationTargetException e) {
				
				e.printStackTrace();
				return null;
			}
			
		} else {
			
			BoundingBox box = entity.getBoundingBox();
			
			return new WrappedBoundingBox(
					entity,
					box.getWidthX(),
					box.getHeight());
		}
	}
	
	private static double getBoxWidth(Object aabb) throws IllegalAccessException {
		return AABB_MAX_X.getDouble(aabb) - AABB_MIN_X.getDouble(aabb);
	}
	
	private static double getBoxHeight(Object aabb) throws IllegalAccessException {
		return AABB_MAX_Y.getDouble(aabb) - AABB_MIN_Y.getDouble(aabb);
	}
	
	/**
	 * Returns true if any of the 8 vertices of the bounding box are inside of the block cache.
	 */
	public boolean intersectsBlockCache(BlockCache cache) {
		
		for (Vector vertex : getVertices()) {
			
			if (cache.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if any of the 8 vertices of the bounding box are inside of the view frustum.
	 */
	public boolean intersectsFrustum(ViewFrustum viewFrustum) {
		
		for (Vector vertex : getVertices()) {
			
			if (viewFrustum.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
}