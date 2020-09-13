package me.gorgeousone.netherview.utils;

public final class WordUtils {
	
	private WordUtils() {}
	
	public static String capitalize(String str) {
		
		final char[] buffer = str.toLowerCase().toCharArray();
		boolean capitalizeNext = true;
		
		for (int i = 0; i < buffer.length; i++) {
			
			final char ch = buffer[i];
			
			if (Character.isWhitespace(ch)) {
				capitalizeNext = true;
				
			} else if (capitalizeNext) {
				
				buffer[i] = Character.toTitleCase(ch);
				capitalizeNext = false;
			}
		}
		return new String(buffer);
	}
}
