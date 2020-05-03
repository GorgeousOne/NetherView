package me.gorgeousone.netherview.viewfrustum;

import me.gorgeousone.netherview.threedstuff.Line;
import org.bukkit.util.Vector;

/**
 * An euclidean line where only points between the start and the end of it can be accessed.
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
