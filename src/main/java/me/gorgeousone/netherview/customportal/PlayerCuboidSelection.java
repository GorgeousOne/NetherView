package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.geometry.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerCuboidSelection {
	
	private final UUID playerId;
	private final World world;
	
	private BlockVec pos1;
	private BlockVec pos2;
	
	public PlayerCuboidSelection(Player player) {
		
		this.playerId = player.getUniqueId();
		this.world = player.getWorld();
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(playerId);
	}
	
	public World getWorld() {
		return world;
	}
	
	public BlockVec getPos1() {
		return pos1;
	}
	
	public void setPos1(BlockVec pos1) {
		this.pos1 = pos1;
	}
	
	public BlockVec getPos2() {
		return pos2;
	}
	
	public void setPos2(BlockVec pos2) {
		this.pos2 = pos2;
	}
	
	public boolean bothPositionsAreSet() {
		return pos1 != null && pos2 != null;
	}
}