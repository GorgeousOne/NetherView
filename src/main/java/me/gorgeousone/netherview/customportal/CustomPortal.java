package me.gorgeousone.netherview.customportal;

import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.entity.Player;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.portal.Portal;

public class CustomPortal extends Portal {
	
	private String name;
	
	public CustomPortal(World world,
	                    AxisAlignedRect portalRect,
	                    Cuboid frameShape,
	                    Cuboid innerShape) {
		super(world, portalRect, frameShape, innerShape, new HashSet<>());
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void setLinkedTo(Player player, Portal counterPortal) {
		
		if (counterPortal instanceof CustomPortal) {
			super.setLinkedTo(player, counterPortal);
		} else {
			throw new IllegalArgumentException("Cannot link custom portal to not custom portal");
		}
	}
}
