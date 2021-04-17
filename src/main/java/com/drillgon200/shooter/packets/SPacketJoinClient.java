package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.entity.PlayerClient;
import com.drillgon200.shooter.gui.GuiIngame;

public class SPacketJoinClient implements IMessage {

	public short playerId;
	
	public SPacketJoinClient() {
	}
	
	public SPacketJoinClient(short id) {
		this.playerId = id;
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		buffer.putShort(playerId);
	}

	@Override
	public void read(ByteBuffer buffer) {
		playerId = buffer.getShort();
	}

	public static class Handler implements IMessageHandler<SPacketJoinClient> {

		@Override
		public void onMessage(SPacketJoinClient m, Connection c) {
			Shooter.addScheduledTask(() -> {
				if(Shooter.state == ConnectionState.CONNECTING){
					Shooter.world.ticksSinceLastStateUpdate = 0;
					Shooter.player = (PlayerClient) Shooter.world.getEntityById(m.playerId);
					Shooter.player.connection = c;
					Shooter.displayGui(new GuiIngame());
					Shooter.setMouseGrabbed(true);
					Shooter.state = ConnectionState.CONNECTED;
				}
			});
		}
		
	}
	
}
