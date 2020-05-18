package me.gorgeousone.netherview;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class DisplayUtils {
	
	public static void removeFakeBlocks(Player player, Map<BlockVec, BlockData> blockCopies) {
		
		World playerWorld = player.getWorld();
		Map<BlockVec, BlockData> updatedBlockCopies = new HashMap<>();
		
		for (BlockVec blockPos : blockCopies.keySet())
			updatedBlockCopies.put(blockPos.clone(), blockPos.toBlock(playerWorld).getBlockData());
		
		displayFakeBlocks(player, updatedBlockCopies);
	}
	
	public static void displayFakeBlocks(Player player, Map<BlockVec, BlockData> blockCopies) {
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		
		World playerWorld = player.getWorld();
		Map<BlockVec, Map<BlockVec, BlockData>> sortedBlockData = getSortedByChunks(blockCopies);
		
		for (Map.Entry<BlockVec, Map<BlockVec, BlockData>> chunkEntry : sortedBlockData.entrySet()) {
			
			BlockVec chunkPos = chunkEntry.getKey();
			Map<BlockVec, BlockData> chunkBlockData = chunkEntry.getValue();
			
			//create an empty multi block change packet
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			fakeBlocksPacket.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkPos.getX(), chunkPos.getZ()));
			
			MultiBlockChangeInfo[] blockInfo = new MultiBlockChangeInfo[chunkBlockData.size()];
			int i = 0;
			
			for (Map.Entry<BlockVec, BlockData> entry : chunkBlockData.entrySet()) {
				
				Location blockLoc = entry.getKey().toLocation(playerWorld);
				blockInfo[i] = new MultiBlockChangeInfo(blockLoc, WrappedBlockData.createData(entry.getValue()));
				i++;
			}
			
			fakeBlocksPacket.getMultiBlockChangeInfoArrays().write(0, blockInfo);
			
			try {
				protocolManager.sendServerPacket(player, fakeBlocksPacket);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Failed to send packet " + fakeBlocksPacket, e);
			}
		}
	}
	
	private static Map<BlockVec, Map<BlockVec, BlockData>> getSortedByChunks(Map<BlockVec, BlockData> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockData>> sortedBlockCopies = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockData> entry : blockCopies.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockVec chunkPos = new BlockVec(blockPos.getX() >> 4, 0, blockPos.getZ() >> 4);
			
			sortedBlockCopies.putIfAbsent(chunkPos, new HashMap<>());
			sortedBlockCopies.get(chunkPos).put(blockPos, entry.getValue());
		}
		
		return sortedBlockCopies;
	}
}
