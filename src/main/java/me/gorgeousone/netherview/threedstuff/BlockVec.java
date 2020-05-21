package me.gorgeousone.netherview.threedstuff;

import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Objects;

public class BlockVec {
	
	private int x;
	private int y;
	private int z;
	
	public BlockVec() {}
	
	public BlockVec(Block block) {
		this(block.getX(), block.getY(), block.getZ());
	}
	
	public BlockVec(Location location) {
		this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}
	
	public BlockVec(Vector vector) {
		this(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
	}
	
	public BlockVec(BlockPosition blockPosition) {
		this(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
	}
	
	public BlockVec(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getZ() {
		return z;
	}
	
	public void setZ(int z) {
		this.z = z;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public BlockVec add(BlockVec other) {
		return add(other.x, other.y, other.z);
	}
	
	public BlockVec add(int dx, int dy, int dz) {
		x += dx;
		y += dy;
		z += dz;
		return this;
	}
	
	public BlockVec subtract(BlockVec other) {
		x -= other.getX();
		y -= other.getY();
		z -= other.getZ();
		return this;
	}
	
	public void multiply(int multiplier) {
		x *= multiplier;
		y *= multiplier;
		z *= multiplier;
	}
	
	public static BlockVec getMinimum(BlockVec v1, BlockVec v2) {
		return new BlockVec(
				Math.min(v1.x, v2.x),
				Math.min(v1.y, v2.y),
				Math.min(v1.z, v2.z));
	}
	
	public static BlockVec getMaximum(BlockVec v1, BlockVec v2) {
		return new BlockVec(
				Math.max(v1.x, v2.x),
				Math.max(v1.y, v2.y),
				Math.max(v1.z, v2.z));
	}
	
	public Vector toVector() {
		return new Vector(x, y, z);
	}
	
	public Location toLocation(World world) {
		return new Location(world, x, y, z);
	}
	
	public Block toBlock(World world) {
		return world.getBlockAt(x, y, z);
	}
	
	@Override
	public BlockVec clone() {
		return new BlockVec(x, y, z);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BlockVec)) {
			return false;
		}
		BlockVec otherVec = (BlockVec) o;
		return x == otherVec.x &&
		       y == otherVec.y &&
		       z == otherVec.z;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}
	
	@Override
	public String toString() {
		return "[" +
		       "x=" + x +
		       ",y=" + y +
		       ",z=" + z +
		       ']';
	}
}