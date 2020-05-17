package me.gorgeousone.netherview;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisplayUtils {
	
	public static void displayFakeBlocks(Player player, Set<BlockCopy> blockCopies) {
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		
		World playerWorld = player.getWorld();
		Map<BlockVec, List<BlockCopy>> sortedBlockCopies = getSortedByChunks(blockCopies);
		
		for (Map.Entry<BlockVec, List<BlockCopy>> entry : sortedBlockCopies.entrySet()) {
			
			BlockVec chunkPos = entry.getKey();
			List<BlockCopy> chunkBlocks = entry.getValue();
			
			//create an empty multi block change packet
			PacketContainer fakeBlockPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			fakeBlockPacket.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkPos.getX(), chunkPos.getZ()));
			
			MultiBlockChangeInfo[] blockInfo = new MultiBlockChangeInfo[chunkBlocks.size()];
			
			for(int i = 0; i < chunkBlocks.size(); i++) {
				
				BlockCopy block = chunkBlocks.get(i);
				Location blockLoc = block.getPosition().toLocation(playerWorld);
				blockInfo[i] = new MultiBlockChangeInfo(blockLoc, WrappedBlockData.createData(block.getBlockData()));
			}
			
			fakeBlockPacket.getMultiBlockChangeInfoArrays().write(0, blockInfo);
			
			try {
				protocolManager.sendServerPacket(player, fakeBlockPacket);
				
			} catch (InvocationTargetException e) {
				throw new RuntimeException(
						"Cannot send packet " + fakeBlockPacket, e);
			}
		}
	}
	
	private static Map<BlockVec, List<BlockCopy>> getSortedByChunks(Set<BlockCopy> blockCopies) {
		
		Map<BlockVec, List<BlockCopy>> sortedBlockCopies = new HashMap<>();
		
		for (BlockCopy blockCopy : blockCopies) {
			
			BlockVec blockPos = blockCopy.getPosition();
			BlockVec chunkPos = new BlockVec(blockPos.getX() >> 4, 0, blockPos.getZ() >> 4);
			
			sortedBlockCopies.putIfAbsent(chunkPos, new ArrayList<>());
			sortedBlockCopies.get(chunkPos).add(blockCopy);
		}
		
		return sortedBlockCopies;
	}
}
