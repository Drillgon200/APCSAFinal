package com.drillgon200.networking.udp;

public class PacketTest implements IMessageUDP {

	public String result;
	public byte[] data;
	
	public PacketTest() {
	}
	
	public PacketTest(String s, int dataSize) {
		result = s;
		data = new byte[dataSize];
		for(int i = 0; i < data.length; i ++){
			data[i] = (byte) (i % 100);
		}
	}
	
	@Override
	public boolean reliable() {
		return true;
	}

	@Override
	public void serialize(Stream s) {
		byte[] bytes;
		if(s.isWriting()){
			bytes = result.getBytes();
			s.serializeByte((byte) bytes.length);
			s.serializeInt(data.length);
		} else {
			bytes = new byte[s.serializeByte((byte) 0)&0xFF];
			data = new byte[s.serializeInt(0)];
		}
		for(int i = 0; i < bytes.length; i ++){
			bytes[i] = s.serializeByte(bytes[i]);
		}
		for(int i = 0; i < data.length; i ++){
			data[i] = s.serializeByte(data[i]);
		}
		if(!s.isWriting()){
			result = new String(bytes);
		}
	}

	public static class Handler implements IMessageHandlerUDP<PacketTest> {

		@Override
		public void onMessage(PacketTest m, MessageContext c) {
			System.out.println("Packet recieved: " + m.result);
			System.out.println(m.data[0] + " " + m.data[m.data.length - 5] + " " + c.side);
		}
		
	}
}
