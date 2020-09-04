package me.gorgeousone.netherview.wrapping.boundingbox;

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

public class BoundingBoxUtils {
	
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
	
	public static EntityBoundingBox getWrappedBoxOf(Entity entity) {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				Object entityAabb = ENTITY_GET_AABB.invoke(NmsUtils.getHandle(entity));
				
				return new EntityBoundingBox(
						entity,
						getBoxWidth(entityAabb),
						getBoxHeight(entityAabb));
				
			} catch (IllegalAccessException | InvocationTargetException e) {
				
				e.printStackTrace();
				return null;
			}
			
		} else {
			
			BoundingBox box = entity.getBoundingBox();
			
			return new EntityBoundingBox(
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
	
	public static boolean boxIntersectsBlockCache(EntityBoundingBox box, BlockCache cache) {
		
		for (Vector vertex : box.getVertices()) {
			
			if (cache.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean boxIntersectsFrustum(EntityBoundingBox box, ViewFrustum viewFrustum) {
		
		for (Vector vertex : box.getVertices()) {
			
			if (viewFrustum.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
}
