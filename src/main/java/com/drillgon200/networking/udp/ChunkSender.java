package com.drillgon200.networking.udp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.zip.CRC32;

public class ChunkSender {

	public static final int MAX_SLICES = 256;
	public static final float timeBetweenSends = 0.1F;
	public static final float bytesPerSecond = 32768;
	
	public UDPConnection connection;
	public DatagramChannel channel;
	
	public boolean sending = false;
	public short chunkId = 0;
	public int chunkSize = 0;
	public int numSlices = 0;
	public int numAckedSlices = 0;
	public boolean[] acked = new boolean[MAX_SLICES];
	public byte[][] chunkData = new byte[MAX_SLICES][];
	public int bandwidthAccumulator = 0;
	public double timeAccumulator = 0;
	public int prevSliceSent = 0;
	public double[] timeLastSent = new double[MAX_SLICES];
	
	public ChunkSender(UDPConnection c) {
		this.channel = c.channel;
		this.connection = c;
	}
	
	public void sendChunk(ByteBuffer buf) throws IOException {
		if(sending)
			throw new PacketException("Already sending a fragmented packet!");
		for(int i = 0; i < acked.length; i ++)
			acked[i] = false;
		bandwidthAccumulator = 0;
		prevSliceSent = 0;
		numAckedSlices = 0;
		int limit = buf.limit();
		int sliceId = 0;
		numSlices = (int) Math.ceil((float)buf.remaining()/1024);
		chunkSize = buf.remaining();
		while(buf.remaining() > 0){
			int amount = Math.min(buf.remaining(), UDPNetworkManager.MAX_PACKET_SIZE);
			buf.limit(buf.position()+amount);
			if(chunkData[sliceId] == null)
				chunkData[sliceId] = new byte[UDPNetworkManager.MAX_PACKET_SIZE];
			buf.get(chunkData[sliceId], 0, buf.remaining());
			buf.limit(limit);
			sliceId ++;
		}
		sending = true;
		sendInitialBurst(buf);
	}
	
	private void sendInitialBurst(ByteBuffer buffer) throws IOException {
		for(int i = 0; i < numSlices; i ++){
			sendSlice(i, buffer);
		}
	}
	
	private void sendSlice(int sliceId, ByteBuffer buf) throws IOException {
		//System.out.println("Sending slice: " + sliceId + " for chunk: " + chunkId);
		timeLastSent[sliceId] = timeAccumulator;
		int size = sliceId == numSlices-1 ? chunkSize % 1024 : 1024;
		if(size == 0)
			size = 1024;
		buf.position(9);
		buf.limit(9+size);
		//buf.put(chunkData[sliceId], 0, size);
		//System.arraycopy is probably a lot faster
		byte[] arr = buf.array();
		System.arraycopy(chunkData[sliceId], 0, arr, 9, size);
		buf.position(buf.position()-5);
		buf.put((byte) UDPNetworkManager.PacketType.RELIABLE_FRAGMENT.ordinal());
		buf.putShort(chunkId);
		buf.put((byte) sliceId);
		buf.put((byte)numSlices);
		int oldPos = buf.position()-5;
		buf.position(oldPos);
		
		CRC32 crc = new CRC32();
		UDPNetworkManager.addMagicProtocolID(crc);
		crc.update(buf);
		buf.position(oldPos-4);
		buf.putInt((int) (crc.getValue()&0xFFFFFFFF));
		buf.position(oldPos-4);
		if(channel.isConnected()){
			channel.write(buf);
		} else {
			channel.send(buf, connection.remoteAddress);
		}
		buf.clear();
	}
	
	public void handleAck(int chunkId, int numSlices, boolean[] acks){
		if(!sending || this.chunkId != chunkId || this.numSlices != numSlices)
			return;
		//System.out.println("Handling acks " + acks[0] + " " + acks[1] + " " + numSlices);
		for(int i = 0; i < numSlices; i ++){
			if(!acked[i] && acks[i]){
				acked[i] = true;
				numAckedSlices ++;
				if(numAckedSlices == numSlices){
					sending = false;
					this.chunkId ++;
					return;
				}
			}
		}
	}
	
	public void update(ByteBuffer buf, double dt) throws IOException {
		timeAccumulator += dt;
		bandwidthAccumulator += (int)(Math.max(dt*bytesPerSecond, 1));
		if(!sending)
			return;
		for(int i = prevSliceSent; i < prevSliceSent+numSlices; i ++){
			int slice = i % numSlices;
			if(acked[i] || (timeAccumulator - timeLastSent[i] <= timeBetweenSends))
				continue;
			int size = slice == numSlices-1 ? chunkSize % 1024 : 1024;
			if(size == 0)
				size = 1024;
			if(size > bandwidthAccumulator){
				prevSliceSent = slice;
				break;
			}
			bandwidthAccumulator -= size;
			sendSlice(slice, buf);
		}
	}
	
}
