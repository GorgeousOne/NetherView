package me.gorgeousone.netherview;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AABBUtils {
	
	private static String VERSION;
	private static Method GET_HANDLE;
	private static Method GET_BOUNDING_BOX;
	
	private static Class NMS_BOUNDING_BOX;
	private static Field BOX_MIN_X;
	private static Field BOX_MIN_Y;
	private static Field BOX_MIN_Z;
	private static Field BOX_MAX_X;
	private static Field BOX_MAX_Y;
	private static Field BOX_MAX_Z;
	
	static {
		VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
		
		try {
			GET_HANDLE = getCraftBukkitClass("entity.CraftEntity").getMethod("getHandle");
			GET_BOUNDING_BOX = getNMSClass("Entity").getMethod("getBoundingBox");
			
			NMS_BOUNDING_BOX = getNMSClass("AxisAlignedBB");
			BOX_MIN_X = NMS_BOUNDING_BOX.getField("a");
			BOX_MIN_Y = NMS_BOUNDING_BOX.getField("b");
			BOX_MIN_Z = NMS_BOUNDING_BOX.getField("c");
			BOX_MAX_X = NMS_BOUNDING_BOX.getField("d");
			BOX_MAX_Y = NMS_BOUNDING_BOX.getField("e");
			BOX_MAX_Z = NMS_BOUNDING_BOX.getField("f");
			
		} catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	public static BoundingBox getBoundingBox(Entity entity) {
		
		try {
			return entity.getBoundingBox();
			
		}catch (Error noSuchMethodE) {

			try {
				Object boundingBox =  GET_BOUNDING_BOX.invoke(GET_HANDLE.invoke(entity));
				
				double minX = BOX_MIN_X.getDouble(boundingBox);
				double minY = BOX_MIN_Y.getDouble(boundingBox);
				double minZ = BOX_MIN_Z.getDouble(boundingBox);
				double maxX = BOX_MAX_X.getDouble(boundingBox);
				double maxY = BOX_MAX_Y.getDouble(boundingBox);
				double maxZ = BOX_MAX_Z.getDouble(boundingBox);
				
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "HELLO HELLO HELLOOOOO!");
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "" + minX + ", " + minY + ", " + minZ);
				
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private static Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + VERSION + nmsClassString);
	}
	
	private static Class<?> getCraftBukkitClass(String cbClassString) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + VERSION + cbClassString);
	}
}
