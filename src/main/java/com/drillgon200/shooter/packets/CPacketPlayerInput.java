package com.drillgon200.shooter.packets;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.entity.PlayerClient;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.util.Vec3f;

public class CPacketPlayerInput implements IMessageUDP {

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
	public boolean reliable() {
		return false;
	}
	
	@Override
	public void serialize(Stream s) {
		posX = s.serializeFloat(posX);
		posY = s.serializeFloat(posY);
		posZ = s.serializeFloat(posZ);
	}
	
	public static class Handler implements IMessageHandlerUDP<CPacketPlayerInput> {

		@Override
		public void onMessage(CPacketPlayerInput m, MessageContext c) {
			ShooterServer.addScheduledTask(() -> {
				c.connection.player.setPos(m.posX, m.posY, m.posZ);
			});
		}
		
	}

}
