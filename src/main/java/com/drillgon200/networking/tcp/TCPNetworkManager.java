package com.drillgon200.networking.tcp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.drillgon200.shooter.Side;

public class TCPNetworkManager {

	public static final int TIMEOUT = 5*1000;
	
	public static Map<Integer, PacketInfo<? extends IMessageTCP>> packets = new HashMap<>();
	public static Map<Class<? extends IMessageTCP>, Integer> idsByMessage = new HashMap<>();
	
	public static <PKT extends IMessageTCP> void registerPacket(Class<PKT> message, Class<? extends IMessageHandlerTCP<PKT>> handler, int id, Side side){
		if(packets.containsKey(id))
			throw new RuntimeException("Already registered packet: " + packets.get(id).message + " with id " + id);
		packets.put(id, new PacketInfo<PKT>(id, newInstance(handler), message, side));
		idsByMessage.put(message, id);
	}
	
	protected static class PacketInfo<PKT extends IMessageTCP> {
		public int id;
		public IMessageHandlerTCP<PKT> handler;
		public Class<PKT> message;
		public Side side;
		
		public PacketInfo(int id, IMessageHandlerTCP<PKT> handler, Class<PKT> message, Side side) {
			this.id = id;
			this.handler = handler;
			this.message = message;
			this.side = side;
		}
		
		public void handleMessage(ByteBuffer buf, TCPConnection c){
			PKT packet = newInstance(message);
			packet.read(buf);
			handler.onMessage(packet, c);
		}
	}
	
	private static <T> T newInstance(Class<T> clazz){
		try {
			return clazz.newInstance();
		} catch(InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to instantiate packet class, does it have a blank constructor?");
		}
	}
}
