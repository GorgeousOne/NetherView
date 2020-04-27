package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class PortalHandler {
	
	private Map<Portal, Portal> runningPortals;
	private Map<Portal, Transform> linkTransforms;
	
	public PortalHandler() {
		runningPortals = new HashMap<>();
		linkTransforms = new HashMap<>();
	}
	
	public void linkPortals(Portal overworldPortal, Portal netherPortal) {
		
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
		
		for (Portal portal : runningPortals.keySet()) {
			if (portal.containsBlock(portalBlock))
				return true;
		}
		
		return false;
	}
	
	public Portal nearestPortal(Location playerLoc) {
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : runningPortals.keySet()) {
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	public Portal getLinkedNetherPortal(Portal portal) {
		return runningPortals.get(portal);
	}
	
	public Transform getLinkTransform(Portal portal) {
		return linkTransforms.get(portal);
	}
}
