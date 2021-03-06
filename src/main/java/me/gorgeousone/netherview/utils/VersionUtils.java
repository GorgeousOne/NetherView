package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;

public final class VersionUtils {
	
	private VersionUtils() {}
	
	public static final String VERSION_STRING = Bukkit.getServer().getClass().getName().split("\\.")[3];
	private static final int[] CURRENT_VERSION_INTS = new int[3];
	
	static {
		String versionStringNumbersOnly = VERSION_STRING.replaceAll("[a-zA-Z]", "");
		System.arraycopy(getVersionAsIntArray(versionStringNumbersOnly, "_"), 0, CURRENT_VERSION_INTS, 0, 3);
	}
	
	public static final boolean IS_LEGACY_SERVER = !serverIsAtOrAbove("1.13.0");
	
	public static boolean isVersionLowerThan(String currentVersion, String requestedVersion) {
		
		int[] currentVersionInts = getVersionAsIntArray(currentVersion, "\\.");
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < Math.min(currentVersionInts.length, requestedVersionInts.length); i++) {
			
			int versionDiff = currentVersionInts[i] - requestedVersionInts[i];
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0) {
				return true;
			}
		}
		
		return requestedVersionInts.length > currentVersionInts.length;
	}
	
	public static boolean serverIsAtOrAbove(String requestedVersion) {
		
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < requestedVersionInts.length; i++) {
			
			int versionDiff = requestedVersionInts[i] - CURRENT_VERSION_INTS[i];
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0){
				return true;
			}
		}
		
		return true;
	}
	
	private static int[] getVersionAsIntArray(String version, String delimiter) {
		
		String[] split = version.split(delimiter);
		
		if (split.length > 3) {
			throw new IllegalArgumentException("Cannot process awfully long version string \"" + version + "\".");
		}
		
		int[] versionInts = new int[split.length];
		
		for (int i = 0; i < versionInts.length; i++) {
			versionInts[i] = Integer.parseInt(split[i]);
		}
		
		return versionInts;
	}
}
