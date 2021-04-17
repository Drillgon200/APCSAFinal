package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.ShooterServer;

public class CPacketJoinServer implements IMessage {

	
	public CPacketJoinServer() {
	}
	
	@Override
	public void write(ByteBuffer buffer) {
	}

	@Override
	public void read(ByteBuffer buffer) {
	}
	
	public static class Handler implements IMessageHandler<CPacketJoinServer> {

		@Override
		public void onMessage(CPacketJoinServer m, Connection c) {
			ShooterServer.addScheduledTask(() -> {
				System.out.println("Trying player join!");
				ShooterServer.tryJoinPlayer(c);
			});
		}
		
	}

}
