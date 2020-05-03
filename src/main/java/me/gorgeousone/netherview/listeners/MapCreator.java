package me.gorgeousone.netherview.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class MapCreator implements Listener {
	
	@EventHandler
	public void onMapInitialize(MapInitializeEvent event) {
		
		MapView mapView = event.getMap();
		mapView.setScale(MapView.Scale.CLOSEST);
		
		mapView.getRenderers().clear();
		mapView.addRenderer(new MapRenderer() {
			@Override
			public void render(MapView map, MapCanvas canvas, Player player) {
				
				for (int x = 0; x < 128; x++) {
					for (int y = 0; y < 128; y++) {
						canvas.setPixel(x, y, (byte) 19);
					}
				}
			}
		});
	}
}
