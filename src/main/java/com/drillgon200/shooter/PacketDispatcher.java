package com.drillgon200.shooter;

import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.NetworkManager;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.packets.*;

public class PacketDispatcher {

	public static boolean registered = false;
	
	public static void registerPackets(){
		if(registered)
			return;
		int i = 0;
		NetworkManager.registerPacket(CPacketJoinServer.class, CPacketJoinServer.Handler.class, i++, Side.SERVER);
		NetworkManager.registerPacket(SPacketJoinClient.class, SPacketJoinClient.Handler.class, i++, Side.CLIENT);
		NetworkManager.registerPacket(SPacketAddEntity.class, SPacketAddEntity.Handler.class, i++, Side.CLIENT);
		NetworkManager.registerPacket(SPacketRemoveEntity.class, SPacketRemoveEntity.Handler.class, i++, Side.CLIENT);
		NetworkManager.registerPacket(SPacketStateUpdate.class, SPacketStateUpdate.Handler.class, i++, Side.CLIENT);
		NetworkManager.registerPacket(CPacketPlayerInput.class, CPacketPlayerInput.Handler.class, i++, Side.SERVER);
		registered = true;
	}
	
	public static void sendTo(Player p, IMessage m){
		if(p instanceof PlayerServer){
			p.connection.sendPacket(m);
		}
	}
	
	public static void sendToAll(IMessage m){
		for(PlayerServer p : ShooterServer.players){
			p.connection.sendPacket(m);
		}
	}
	
	public static void sendToServer(IMessage m){
		Shooter.connection.serverConnection.sendPacket(m);
	}

	public static void sendToAllExcept(IMessage m, PlayerServer... ents) {
		l:
		for(PlayerServer p : ShooterServer.players){
			for(int i = 0; i < ents.length; i ++){
				if(ents[i] == p)
					continue l;
			}
			p.connection.sendPacket(m);
		}
	}
}
