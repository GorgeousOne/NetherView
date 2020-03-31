package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class PortalHandler {
	
	private Map<PortalStructure, PortalStructure> runningPortals;
	private Map<PortalStructure, Transform> linkTransforms;
	
	public PortalHandler() {
		runningPortals = new HashMap<>();
		linkTransforms = new HashMap<>();
	}
	
	public void linkPortals(PortalStructure overworldPortal, PortalStructure netherPortal) {
		
		Transform linTransform = new Transform();
		
		Vector dist = overworldPortal.getLocation().toVector().subtract(netherPortal.getLocation().toVector());
		linTransform.setTranslation(new BlockVec(dist));
		linTransform.setRotCenter(new BlockVec(netherPortal.getPortalRect().getMin()));
		
		if (overworldPortal.getAxis() == netherPortal.getAxis()) {
			linTransform.setRotY180Deg();
		} else {
			if (netherPortal.getAxis() == Axis.X)
				linTransform.setRotY90DegRight();
			else
				linTransform.setRotY90DegLeft();
		}
		
		linkTransforms.put(overworldPortal, linTransform);
		runningPortals.put(overworldPortal, netherPortal);
	}
	
	public boolean containsPortalWithBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return false;
		
		for (PortalStructure portal : runningPortals.keySet()) {
			if (portal.containsBlock(portalBlock))
				return true;
		}
		
		return false;
	}
	
	public PortalStructure nearestPortal(Location playerLoc) {
		
		PortalStructure nearestPortal = null;
		double minDist = -1;
		
		for (PortalStructure portal : runningPortals.keySet()) {
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public PortalStructure getLinkedNetherPortal(PortalStructure portal) {
		return runningPortals.get(portal);
	}
	
	public Transform getLinkTransform(PortalStructure portal) {
		return linkTransforms.get(portal);
	}
}
