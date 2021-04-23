package com.drillgon200.networking.udp;

public class PacketServerClientTest implements IMessageUDP {

	public PacketServerClientTest() {
	}
	
	@Override
	public boolean reliable() {
		return false;
	}

	@Override
	public void serialize(Stream s) {
		
	}
	
	public static class Handler implements IMessageHandlerUDP<PacketServerClientTest> {

		@Override
		public void onMessage(PacketServerClientTest m, MessageContext c) {
			System.out.println("Server To Client on side " + c.side);
		}
		
	}

}