package com.drillgon200.shooter;

import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.UDPNetworkManager;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.packets.CPacketJoinServer;
import com.drillgon200.shooter.packets.CPacketPlayerInput;
import com.drillgon200.shooter.packets.SPacketAddEntity;
import com.drillgon200.shooter.packets.SPacketJoinClient;
import com.drillgon200.shooter.packets.SPacketRemoveEntity;
import com.drillgon200.shooter.packets.SPacketStateUpdate;

public class PacketDispatcher {

	public static boolean registered = false;
	
	public static void registerPackets(){
		if(registered)
			return;
		UDPNetworkManager.registerPacket(CPacketJoinServer.class, CPacketJoinServer.Handler.class, Side.SERVER);
		UDPNetworkManager.registerPacket(SPacketJoinClient.class, SPacketJoinClient.Handler.class, Side.CLIENT);
		UDPNetworkManager.registerPacket(SPacketAddEntity.class, SPacketAddEntity.Handler.class, Side.CLIENT);
		UDPNetworkManager.registerPacket(SPacketRemoveEntity.class, SPacketRemoveEntity.Handler.class, Side.CLIENT);
		UDPNetworkManager.registerPacket(SPacketStateUpdate.class, SPacketStateUpdate.Handler.class, Side.CLIENT);
		UDPNetworkManager.registerPacket(CPacketPlayerInput.class, CPacketPlayerInput.Handler.class, Side.SERVER);
		registered = true;
	}
	
	public static void sendTo(Player p, IMessageUDP m){
		if(p instanceof PlayerServer){
			p.connection.sendMessage(m);
		}
	}
	
	public static void sendToAll(IMessageUDP m){
		for(PlayerServer p : ShooterServer.players){
			p.connection.sendMessage(m);
		}
	}
	
	public static void sendToServer(IMessageUDP m){
		Shooter.connection.serverConnection.sendMessage(m);
	}

	public static void sendToAllExcept(IMessageUDP m, PlayerServer... ents) {
		l:
		for(PlayerServer p : ShooterServer.players){
			for(int i = 0; i < ents.length; i ++){
				if(ents[i] == p)
					continue l;
			}
			p.connection.sendMessage(m);
		}
	}
}
