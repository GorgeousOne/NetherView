package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;

public final class VersionUtils {
	
	private VersionUtils() {}
	
	public static final String VERSION_STRING = Bukkit.getServer().getClass().getName().split("\\.")[3];
	
	private static final int VERSION_INT_COUNT = 3;
	private static final int[] CURRENT_VERSION_INTS = new int[VERSION_INT_COUNT];
	
	static {
		String versionStringNumbersOnly = VERSION_STRING.replaceAll("[a-zA-Z]", "");
		System.arraycopy(getVersionAsIntArray(versionStringNumbersOnly, "_"), 0, CURRENT_VERSION_INTS, 0, VERSION_INT_COUNT);
	}
	
	public static final boolean IS_LEGACY_SERVER = !serverIsAtOrAbove("1.13.0");
	
	public static boolean versionIsLowerThan(String version1, String version2) {
		
		int[] versionInts1 = getVersionAsIntArray(version1, "\\.");
		int[] versionInts2 = getVersionAsIntArray(version2, "\\.");
		
		for (int i = 0; i < versionInts1.length; i++) {
			
			if (versionInts1[i] < versionInts2[i]) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean serverIsAtOrAbove(String fullVersionString) {
		
		int[] readVersionInts = getVersionAsIntArray(fullVersionString, "\\.");
		
		for (int i = 0; i < VERSION_INT_COUNT; i++) {
			
			if (CURRENT_VERSION_INTS[i] < readVersionInts[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	private static int[] getVersionAsIntArray(String version, String delimiter) {
		
		String[] split = version.split(delimiter);
		
		if (split.length != VERSION_INT_COUNT) {
			throw new IllegalArgumentException("Cannot process version string \"" + version + "\".");
		}
		
		int[] versionInts = new int[VERSION_INT_COUNT];
		
		for (int i = 0; i < 3; i++) {
			versionInts[i] = Integer.parseInt(split[i]);
		}
		
		return versionInts;
	}
}
