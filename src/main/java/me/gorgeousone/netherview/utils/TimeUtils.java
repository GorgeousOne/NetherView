package me.gorgeousone.netherview.utils;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
	
	public static long getTicksTillNextMinute() {
		LocalTime now = LocalTime.now();
		LocalTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
		return now.until(nextMinute, ChronoUnit.MILLIS) / 50;
	}
}
