package me.gorgeousone.netherview.customportal;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerClickListener implements Listener {
	
	private final PlayerSelectionHandler selectionHandler;
	
	public PlayerClickListener(PlayerSelectionHandler selectionHandler) {
		this.selectionHandler = selectionHandler;
	}
	
	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		
		Action action = event.getAction();
		
		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK || isOffHandClick(event)) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!player.hasPermission(NetherViewPlugin.CUSTOM_PORTAL_PERM)) {
			return;
		}
		
		ItemStack itemInHand = getHandItem(player);
		
		//TODO add custom item
		if (itemInHand == null || itemInHand.getType() != Material.BLAZE_ROD) {
			return;
		}
		
		event.setCancelled(true);
		BlockVec clickedPos = new BlockVec(event.getClickedBlock());
		PlayerCuboidSelection selection = selectionHandler.getOrCreateCuboidSelection(player);
		
		if (action == Action.LEFT_CLICK_BLOCK) {
			
			if (clickedPos.equals(selection.getPos1())) {
				return;
			}
			
			selection.setPos1(clickedPos);
			player.sendMessage("pos 1");
//                       MessageUtils.sendInfo(player, Message.SET_FIRST_CUBOID_POSITION, clickedPos.toString());
		
		} else {
			
			if (clickedPos.equals(selection.getPos2())) {
				return;
			}
			
			selection.setPos2(clickedPos);
			player.sendMessage("pos 2");
//                       MessageUtils.sendInfo(player, Message.SET_SECOND_CUBOID_POSITION, clickedPos.toString());
		}

//             if (selection.bothPositionsAreSet()) {
//                     Cuboid cuboid = new Cuboid(selection.getPos1(), selection.getPos2());
//                     String cuboidSize = cuboid.getWidthX() + "x" + cuboid.getHeight() + "x" + cuboid.getWidthZ();
//                     MessageUtils.sendInfo(player, Message.SELECTION_SIZE_INFO, cuboidSize);
//             }
	}
	
	private boolean isOffHandClick(PlayerInteractEvent event) {
		
		try {
			return event.getHand() == EquipmentSlot.OFF_HAND;
		} catch (NoSuchMethodError e) {
			return false;
		}
	}
	
	private ItemStack getHandItem(Player player) {
		
		try {
			return player.getInventory().getItemInMainHand();
		} catch (NoSuchMethodError e) {
			return player.getItemInHand();
		}
	}
}