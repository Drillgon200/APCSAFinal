package com.drillgon200.networking;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.drillgon200.shooter.Side;

public class NetworkManager {

	public static final int TIMEOUT = 5*1000;
	
	public static Map<Integer, PacketInfo<? extends IMessage>> packets = new HashMap<>();
	public static Map<Class<? extends IMessage>, Integer> idsByMessage = new HashMap<>();
	
	public static <PKT extends IMessage> void registerPacket(Class<PKT> message, Class<? extends IMessageHandler<PKT>> handler, int id, Side side){
		if(packets.containsKey(id))
			throw new RuntimeException("Already registered packet: " + packets.get(id).message + " with id " + id);
		packets.put(id, new PacketInfo<PKT>(id, newInstance(handler), message, side));
		idsByMessage.put(message, id);
	}
	
	protected static class PacketInfo<PKT extends IMessage> {
		public int id;
		public IMessageHandler<PKT> handler;
		public Class<PKT> message;
		public Side side;
		
		public PacketInfo(int id, IMessageHandler<PKT> handler, Class<PKT> message, Side side) {
			this.id = id;
			this.handler = handler;
			this.message = message;
			this.side = side;
		}
		
		public void handleMessage(ByteBuffer buf, Connection c){
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
			throw new RuntimeException("Failed to instantiate packet handler class");
		}
	}
}
