package me.gorgeousone.netherview.wrapper;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.NmsUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
	private static Field AABB_MIN_Z;
	
	private static Field AABB_MAX_X;
	private static Field AABB_MAX_Y;
	private static Field AABB_MAX_Z;
	
	static {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				ENTITY_GET_AABB = NmsUtils.getNmsClass("Entity").getDeclaredMethod("getBoundingBox");
				Class<?> aabbClass = NmsUtils.getNmsClass("AxisAlignedBB");
				
				AABB_MIN_X = aabbClass.getDeclaredField("a");
				AABB_MIN_Y = aabbClass.getDeclaredField("b");
				AABB_MIN_Z = aabbClass.getDeclaredField("c");
				AABB_MAX_X = aabbClass.getDeclaredField("d");
				AABB_MAX_Y = aabbClass.getDeclaredField("e");
				AABB_MAX_Z = aabbClass.getDeclaredField("f");
				
			} catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final double widthX;
	private final double widthZ;
	private final double height;
	private final List<Vector> vertices;
	
	public WrappedBoundingBox(Entity entity, Location entityLoc, double widthX, double height, double widthZ) {
		
		this.widthX = widthX;
		this.widthZ = widthZ;
		this.height = height;
		
		//dunno, paintings are a bit off
		if (entity.getType() == EntityType.PAINTING && VersionUtils.IS_LEGACY_SERVER) {
			entityLoc.subtract(0, height/2, 0);
		}
		
		Vector min = entityLoc.clone().subtract(
				widthX / 2,
				0,
				widthZ / 2).toVector();
		
		Vector max = entityLoc.clone().add(
				widthX / 2,
				height,
				widthZ / 2).toVector();
		
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
	
	public double getWidthX() {
		return widthX;
	}
	
	public double getWidthZ() {
		return widthZ;
	}
	
	public double getHeight() {
		return height;
	}
	
	/**
	 * Returns the 8 vertices of the entity's bounding box
	 */
	public List<Vector> getVertices() {
		return vertices;
	}
	
	public static WrappedBoundingBox of(Entity entity) {
		return of(entity, entity.getLocation());
	}
	
	public static WrappedBoundingBox of(Entity entity, Location entityLoc) {
		
		if (VersionUtils.IS_LEGACY_SERVER) {
			
			try {
				Object entityAabb = ENTITY_GET_AABB.invoke(NmsUtils.getHandle(entity));
				
				return new WrappedBoundingBox(
						entity,
						entityLoc,
						getBoxWidthX(entityAabb),
						getBoxHeight(entityAabb),
						getBoxWidthZ(entityAabb));
				
			} catch (IllegalAccessException | InvocationTargetException e) {
				
				e.printStackTrace();
				return null;
			}
			
		} else {
			
			BoundingBox box = entity.getBoundingBox();
			
			return new WrappedBoundingBox(
					entity,
					entityLoc,
					box.getWidthX(),
					box.getHeight(),
					box.getWidthZ());
		}
	}
	
	private static double getBoxWidthX(Object aabb) throws IllegalAccessException {
		return AABB_MAX_X.getDouble(aabb) - AABB_MIN_X.getDouble(aabb);
	}
	
	private static double getBoxWidthZ(Object aabb) throws IllegalAccessException {
		return AABB_MAX_Z.getDouble(aabb) - AABB_MIN_Z.getDouble(aabb);
	}
	
	private static double getBoxHeight(Object aabb) throws IllegalAccessException {
		return AABB_MAX_Y.getDouble(aabb) - AABB_MIN_Y.getDouble(aabb);
	}
	
	/**
	 * Returns true if any of the 8 vertices of the bounding box are inside of the block cache.
	 */
	public boolean intersectsPortal(Portal portal) {
		
		for (Vector vertex : getVertices()) {
			
			if (portal.contains(vertex)) {
				return true;
			}
		}
		return false;
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