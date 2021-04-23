package com.drillgon200.networking.udp;

public class PacketChunkAcks implements IMessageUDP {

	public short chunkId;
	public int numSlices;
	public boolean[] acks;
	
	public PacketChunkAcks() {
	}
	
	public PacketChunkAcks(short chunkId, int numSlices, boolean[] acks) {
		this.chunkId = chunkId;
		this.numSlices = numSlices;
		this.acks = acks;
	}
	
	@Override
	public boolean reliable() {
		return false;
	}

	@Override
	public void serialize(Stream s) {
		chunkId = s.serializeShort(chunkId);
		numSlices = s.serializeBits(numSlices, 8);
		if(!s.isWriting())
			acks = new boolean[numSlices];
		for(int i = 0; i < numSlices; i ++){
			acks[i] = s.serializeBoolean(acks[i]);
		}
			
	}
	
	public static class Handler implements IMessageHandlerUDP<PacketChunkAcks> {

		@Override
		public void onMessage(PacketChunkAcks m, MessageContext c) {
			c.connection.reliableMessageHandler.chunkSender.handleAck(m.chunkId, m.numSlices, m.acks);
		}
		
	}

}
