package com.drillgon200.shooter.packets;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.ShooterServer;

public class CPacketJoinServer implements IMessageUDP {

	
	public CPacketJoinServer() {
	}
	
	@Override
	public boolean reliable() {
		return true;
	}
	
	@Override
	public void serialize(Stream s) {
	}
	
	public static class Handler implements IMessageHandlerUDP<CPacketJoinServer> {

		@Override
		public void onMessage(CPacketJoinServer m, MessageContext c) {
			ShooterServer.addScheduledTask(() -> {
				System.out.println("Trying player join!");
				ShooterServer.tryJoinPlayer(c);
			});
		}
		
	}

}
