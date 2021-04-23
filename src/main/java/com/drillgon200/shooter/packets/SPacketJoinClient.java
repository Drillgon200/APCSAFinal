package com.drillgon200.shooter.packets;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.entity.PlayerClient;
import com.drillgon200.shooter.gui.GuiIngame;

public class SPacketJoinClient implements IMessageUDP {

	public short playerId;
	
	public SPacketJoinClient() {
	}
	
	public SPacketJoinClient(short id) {
		this.playerId = id;
	}
	
	@Override
	public boolean reliable() {
		return true;
	}
	
	@Override
	public void serialize(Stream s) {
		playerId = s.serializeShort(playerId);
	}

	public static class Handler implements IMessageHandlerUDP<SPacketJoinClient> {

		@Override
		public void onMessage(SPacketJoinClient m, MessageContext c) {
			Shooter.addScheduledTask(() -> {
				if(Shooter.state == ConnectionState.CONNECTING){
					Shooter.world.ticksSinceLastStateUpdate = 0;
					Shooter.player = (PlayerClient) Shooter.world.getEntityById(m.playerId);
					Shooter.player.connection = c.connection;
					c.connection.player = Shooter.player;
					Shooter.displayGui(new GuiIngame());
					Shooter.setMouseGrabbed(true);
					Shooter.state = ConnectionState.CONNECTED;
				}
			});
		}
		
	}
	
}
