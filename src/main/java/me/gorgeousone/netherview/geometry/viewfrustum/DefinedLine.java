package me.gorgeousone.netherview.geometry.viewfrustum;

import me.gorgeousone.netherview.geometry.Line;
import org.bukkit.util.Vector;

/**
 * An euclidean line where only points between the given start and end can be accessed.
 */
public class DefinedLine extends Line {
	
	public DefinedLine(Vector start, Vector end) {
		super(start, end);
	}
	
	@Override
	public Vector getPoint(double d) {
		return d < 0 || d > 1 ? null : super.getPoint(d);
	}
}
