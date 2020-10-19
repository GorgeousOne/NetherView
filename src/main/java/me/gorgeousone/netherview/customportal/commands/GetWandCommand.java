package me.gorgeousone.netherview.customportal.commands;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import me.gorgeousone.netherview.message.Message;
import me.gorgeousone.netherview.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GetWandCommand extends BasicCommand {
	
	public GetWandCommand(ParentCommand parent) {
		super("wand", NetherViewPlugin.PORTAL_WAND_PERM, true, parent);
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		player.getInventory().addItem(new ItemStack(Material.BLAZE_ROD));
		MessageUtils.sendInfo(player, Message.WAND_INFO);
	}
}
