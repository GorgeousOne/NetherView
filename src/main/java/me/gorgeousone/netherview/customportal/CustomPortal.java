package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.World;

import java.util.HashSet;

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
	public void setLinkedTo(Portal counterPortal) {
		
		if (counterPortal instanceof CustomPortal) {
			super.setLinkedTo(counterPortal);
		}else {
			throw new IllegalArgumentException("Cannot link custom portal to not custom portal");
		}
	}
}
