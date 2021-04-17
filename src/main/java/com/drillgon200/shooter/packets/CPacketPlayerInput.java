package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.entity.PlayerClient;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.util.Vec3f;

public class CPacketPlayerInput implements IMessage {

	public float posX;
	public float posY;
	public float posZ;
	
	public CPacketPlayerInput() {
	}
	
	public CPacketPlayerInput(PlayerClient player) {
		Vec3f pos = player.getInterpolatedPos(1);
		posX = pos.x;
		posY = pos.y;
		posZ = pos.z;
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		buffer.putFloat(posX);
		buffer.putFloat(posY);
		buffer.putFloat(posZ);
	}

	@Override
	public void read(ByteBuffer buffer) {
		posX = buffer.getFloat();
		posY = buffer.getFloat();
		posZ = buffer.getFloat();
	}
	
	public static class Handler implements IMessageHandler<CPacketPlayerInput> {

		@Override
		public void onMessage(CPacketPlayerInput m, Connection c) {
			ShooterServer.addScheduledTask(() -> {
				for(PlayerServer p : ShooterServer.players){
					if(p.connection == c){
						p.setPos(m.posX, m.posY, m.posZ);
						return;
					}
				}
			});
		}
		
	}

}
