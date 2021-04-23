package com.drillgon200.networking.udp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import com.drillgon200.shooter.Side;

public class UDPNetworkManager {
	
	public static final ByteBuffer PROTOCOL_VERSION = ByteBuffer.allocate(8);
	static {
		PROTOCOL_VERSION.order(ByteOrder.LITTLE_ENDIAN);
		PROTOCOL_VERSION.putLong(0L);
		PROTOCOL_VERSION.rewind();
	}
	public static final int MAX_PACKET_ID = 255;
	public static final int TIMEOUT = 5*1000;
	//Actual max packet size sent may be a little over, this is just a ballpark estimate of how big a packet should be.
	public static final int MAX_PACKET_SIZE = 1024;
	
	public static int currentPacketId = 0;
	
	public static Map<Integer, PacketInfo<? extends IMessageUDP>> packets = new HashMap<>();
	public static Map<Class<? extends IMessageUDP>, Integer> idsByMessage = new HashMap<>();
	
	static {
		registerDefaults();
	}
	
	public static <PKT extends IMessageUDP> void registerPacket(Class<PKT> message, Class<? extends IMessageHandlerUDP<PKT>> handler, Side side){
		int id = currentPacketId++;
		if(id > MAX_PACKET_ID)
			throw new RuntimeException("Max packet IDs reached!");
		if(packets.containsKey(id))
			throw new RuntimeException("Already registered packet: " + packets.get(id).message + " with id " + id);
		packets.put(id, new PacketInfo<PKT>(id, newInstance(handler), message, side));
		idsByMessage.put(message, id);
	}
	
	private static void registerDefaults(){
		registerPacket(PacketChunkAcks.class, PacketChunkAcks.Handler.class, Side.ALL);
	}
	
	protected static class PacketInfo<PKT extends IMessageUDP> {
		public int id;
		public IMessageHandlerUDP<PKT> handler;
		public Class<PKT> message;
		public Side side;
		
		public PacketInfo(int id, IMessageHandlerUDP<PKT> handler, Class<PKT> message, Side side) {
			this.id = id;
			this.handler = handler;
			this.message = message;
			this.side = side;
		}
		
		public IMessageUDP newMessage(){
			return newInstance(message);
		}
		
		public void handleMessage(ReadStream s, MessageContext c){
			PKT packet = newInstance(message);
			packet.serialize(s);
			handler.onMessage(packet, c);
		}

		@SuppressWarnings("unchecked")
		public void onMessage(IMessageUDP iMessageUDP, MessageContext c) {
			handler.onMessage((PKT) iMessageUDP, c);
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
	
	public static enum PacketType {
		FRAGMENT,
		RELIABLE_FRAGMENT,
		MESSAGE,
		RELIABLE_MESSAGE;
	}

	public static void addMagicProtocolID(CRC32 crc) {
		crc.update(PROTOCOL_VERSION);
		PROTOCOL_VERSION.rewind();
	}
	
	public static boolean sequenceLessThan(int s1, int s2){
		return sequenceGreaterThan(s2, s1);
	}
	
	public static boolean sequenceGreaterThan(int s1, int s2){
		return ((s1 > s2) && (s1-s2 <= 32768)) || ((s1 < s2) && (s2-s1 > 32768));
	}
	
	public static int sequenceDifference(int s1, int s2){
		if(Math.abs(s1-s2) >= 32768){
			if(s1 > s2)
				s2 += 65536;
			else
				s1 += 65536;
		}
		return s1 - s2;
	}
}
