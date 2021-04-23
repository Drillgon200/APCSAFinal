package com.drillgon200.networking.udp;

import java.nio.ByteBuffer;

import com.drillgon200.networking.udp.UDPNetworkManager.PacketInfo;

public class ChunkReceiver {

	public static final int MAX_SLICES = 256;
	public static final float timeBetweenAcks = 0.1F;
	
	public UDPConnection connection;
	
	public boolean receiving = false;
	public boolean readyToRead = false;
	public short chunkId = 0;
	public int chunkSize = 0;
	public int numSlices = 0;
	public int numReceivedSlices = 0;
	public int prevNumSlices = 0;
	public boolean ackPrevChunk = false;
	public boolean[] received = new boolean[MAX_SLICES];
	public byte[][] chunkData = new byte[MAX_SLICES][];
	public double timeAccumulator = 0;
	public double lastAckTime = 0;
	
	public ChunkReceiver(UDPConnection c) {
		this.connection = c;
	}
	
	public void processSlice(int chunkId, int sliceId, int numSlices, ByteBuffer data, MessageContext c){
		if(!receiving && chunkId == this.chunkId-1 && prevNumSlices != 0){
			ackPrevChunk = true;
			return;
		}
		if(this.chunkId != chunkId ||
				(numReceivedSlices > 0 && this.numSlices != numSlices))
			return;
		this.numSlices = numSlices;
		if(sliceId < 0 || sliceId >= numSlices ||
				received[sliceId] ||
				(sliceId != (numSlices-1) && data.remaining() != UDPNetworkManager.MAX_PACKET_SIZE) ||
				data.remaining() > UDPNetworkManager.MAX_PACKET_SIZE){
			numSlices = 0;
			return;
		}
			
		//Done checks, this packet is new and valid data
		receiving = true;
		numReceivedSlices ++;
		received[sliceId] = true;
		chunkSize += data.remaining();
		if(chunkData[sliceId] == null)
			chunkData[sliceId] = new byte[UDPNetworkManager.MAX_PACKET_SIZE];
		//data.get(chunkData[sliceId], 0, data.remaining());
		byte[] arr = data.array();
		System.arraycopy(arr, data.position(), chunkData[sliceId], 0, data.remaining());
		
		if(numReceivedSlices == numSlices){
			reconstructAndHandleChunk(data, c);
			advance();
		}
	}
	
	public void receiveSlicePacket(ByteBuffer buf, MessageContext c){
		int chunkId = buf.getShort() & 0xFFFFFFFF;
		int sliceId = buf.get() & 0xFF;
		int numSlices = buf.get() & 0xFF;
		processSlice(chunkId, sliceId, numSlices, buf, c);
	}
	
	public void reconstructAndHandleChunk(ByteBuffer data, MessageContext c){
		data.clear();
		data.limit(chunkSize);
		byte[] arr = data.array();
		for(int i = 0; i < numSlices; i ++){
			//data.put(chunkData[i], 0, Math.min(1024, data.remaining()));
			int size = Math.min(1024, data.remaining());
			System.arraycopy(chunkData[i], 0, arr, data.position(), size);
			data.position(data.position()+size);
		}
		data.flip();
		ReadStream stream = new ReadStream(new BitReader(data));
		try {
			int id = stream.serializeBits(0, 8);
			PacketInfo<? extends IMessageUDP> info = UDPNetworkManager.packets.get(id);
			if(info == null){
				throw new PacketException("Unregistered packet: " + id);
			}
			info.handleMessage(stream, c);
		} catch(PacketException e2){
			e2.printStackTrace();
			data.clear();
		}
	}
	
	public void advance(){
		chunkId ++;
		for(int i = 0; i < received.length; i ++){
			received[i] = false;
		}
		chunkSize = 0;
		prevNumSlices = numSlices;
		numSlices = 0;
		numReceivedSlices = 0;
		receiving = false;
	}
	
	public void sendAck(){
		if(timeAccumulator - lastAckTime < timeBetweenAcks)
			return;
		if(ackPrevChunk && prevNumSlices != 0){
			ackPrevChunk = false;
			lastAckTime = timeAccumulator;
			boolean[] oldAcks = new boolean[prevNumSlices];
			for(int i = 0; i < prevNumSlices; i ++){
				oldAcks[i] = true;
			}
			connection.sendMessage(new PacketChunkAcks((short)(chunkId-1), prevNumSlices, oldAcks));
		}
		if(receiving){
			lastAckTime = timeAccumulator;
			connection.sendMessage(new PacketChunkAcks(chunkId, numSlices, received));
		}
	}
	
	public void update(double dt){
		timeAccumulator += dt;
		sendAck();
	}
}
