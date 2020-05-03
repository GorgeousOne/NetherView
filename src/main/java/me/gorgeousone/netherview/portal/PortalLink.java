package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.blockcache.Transform;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.Map;

public class PortalLink {
	
	private Portal portal;
	private Portal counterPortal;
	private Transform linkTransform;
	private Map.Entry<BlockCache, BlockCache> copiedCaches;
	
	public PortalLink(Portal portal, Portal counterPortal, Map.Entry<BlockCache, BlockCache> counterCaches) {
		
		this.portal = portal;
		this.counterPortal = counterPortal;
		
		calculateTransformBetweenPortals();
		
		copiedCaches = new AbstractMap.SimpleEntry<BlockCache, BlockCache>(
				BlockCacheFactory.getTransformed(counterCaches.getKey(), linkTransform),
				BlockCacheFactory.getTransformed(counterCaches.getValue(), linkTransform));
		
	}
	
	public Portal getCounterPortal() {
		return counterPortal;
	}
	
	/**
	 * Returns the transform that is needed to move and rotate blocks from the counter portal to the start portal
	 */
	public Transform getTransform() {
		return linkTransform;
	}
	
	public BlockCache getCache(boolean isPlayerBehindPortal) {
		return isPlayerBehindPortal ? copiedCaches.getValue() : copiedCaches.getKey();
	}
	
	private void calculateTransformBetweenPortals() {
		
		Vector distance = portal.getLocation().toVector().subtract(counterPortal.getLocation().toVector());
		
		linkTransform = new Transform();
		linkTransform.setTranslation(new BlockVec(distance));
		linkTransform.setRotCenter(new BlockVec(counterPortal.getPortalRect().getMin()));
		
		//during the rotation some weird shifts happen
		//I did not figure out where they come from, for now the translations are a good workaround
		if (portal.getAxis() == counterPortal.getAxis()) {
			linkTransform.setRotY180Deg();
			
			int portalBlockWidth = (int) portal.getPortalRect().width() - 1;
			
			if (counterPortal.getAxis() == Axis.X)
				linkTransform.translate(new BlockVec(portalBlockWidth, 0, 0));
			else
				linkTransform.translate(new BlockVec(0, 0, portalBlockWidth));
			
		}else if (counterPortal.getAxis() == Axis.X) {
			linkTransform.setRotY90DegRight();
		    linkTransform.translate(new BlockVec(0, 0, 1));
		
		} else {
			linkTransform.setRotY90DegLeft();
			linkTransform.translate(new BlockVec(1, 0, 0));
		}
	}
}
