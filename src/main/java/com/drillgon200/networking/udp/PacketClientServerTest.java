package com.drillgon200.networking.udp;

public class PacketClientServerTest implements IMessageUDP {

	public PacketClientServerTest() {
	}
	
	@Override
	public boolean reliable() {
		return false;
	}

	@Override
	public void serialize(Stream s) {
		
	}
	
	public static class Handler implements IMessageHandlerUDP<PacketClientServerTest> {

		@Override
		public void onMessage(PacketClientServerTest m, MessageContext c) {
			System.out.println("Client To Server on side " + c.side);
		}
		
	}

}
