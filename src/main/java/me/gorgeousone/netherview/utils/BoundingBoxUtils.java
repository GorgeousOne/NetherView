package me.gorgeousone.netherview.utils;

import me.gorgeousone.netherview.wrapping.EntityBoundingBox;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BoundingBoxUtils {
	
	private static Method ENTITY_GET_AABB;
	private static Field AABB_MIN_X;
	private static Field AABB_MIN_Y;
	private static Field AABB_MIN_Z;
	private static Field AABB_MAX_X;
	private static Field AABB_MAX_Y;
	private static Field AABB_MAX_Z;

	static {

		try {
			ENTITY_GET_AABB	= NmsUtils.getNMSClass("Entity").getDeclaredMethod("getBoundingBox");
			
			Class<?> aabbClass = NmsUtils.getNMSClass("AxisAlignedBB");
			AABB_MIN_X = aabbClass.getDeclaredField("a");
			AABB_MIN_Y = aabbClass.getDeclaredField("b");
			AABB_MIN_Z = aabbClass.getDeclaredField("c");
			AABB_MAX_X = aabbClass.getDeclaredField("d");
			AABB_MAX_Y = aabbClass.getDeclaredField("e");
			AABB_MAX_Z = aabbClass.getDeclaredField("g");
			
		} catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	public static EntityBoundingBox getWrappedBoxOf(Entity entity) {
	
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				Object entityAabb = ENTITY_GET_AABB.invoke(NmsUtils.getHandle(entity));
				
				return new EntityBoundingBox(
						entity,
						getBoxWidthX(entityAabb),
						getBoxHeight(entityAabb),
						getBoxWidthZ(entityAabb));
				
			} catch (IllegalAccessException | InvocationTargetException e) {
				
				e.printStackTrace();
				return null;
			}
		
		}else {
			
			BoundingBox box = entity.getBoundingBox();
			
			return new EntityBoundingBox(
					entity,
					box.getWidthX(),
					box.getHeight(),
					box.getWidthZ());
		}
	}
	
	private static double getBoxWidthX(Object aabb) throws IllegalAccessException {
		return AABB_MAX_X.getDouble(aabb) - AABB_MIN_X.getDouble(aabb);
	}
	
	private static double getBoxHeight(Object aabb) throws IllegalAccessException {
		return AABB_MAX_Y.getDouble(aabb) - AABB_MIN_Y.getDouble(aabb);
	}
	
	private static double getBoxWidthZ(Object aabb) throws IllegalAccessException {
		return AABB_MAX_Z.getDouble(aabb) - AABB_MIN_Z.getDouble(aabb);
	}
}
