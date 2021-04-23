package com.drillgon200.networking.udp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import com.drillgon200.networking.udp.UDPNetworkManager.PacketInfo;

public class FragmentHandler {

	public static final int maxEntries = 256;
	
	public short unreliableFragmentSequenceNumber = 0;
	
	public int latestSequence = 0;
	public final int[] sequence = new int[maxEntries];
	public final Entry[] entries = new Entry[maxEntries];
	
	public FragmentHandler() {
		for(int i = 0; i < sequence.length; i ++)
			sequence[i] = 0xFFFFFFFF;
		for(int i = 0; i < entries.length; i ++)
			entries[i] = new Entry();
	}
	
	public void advance(int seq){
		if(!UDPNetworkManager.sequenceGreaterThan(seq, latestSequence)){
			return;
		}
		int oldestSequence = seq - maxEntries + 1;
		for(int i = 0; i < maxEntries; i ++){
			if(sequence[i] != 0xFFFFFFFF && UDPNetworkManager.sequenceLessThan(sequence[i], oldestSequence)){
				sequence[i] = 0xFFFFFFFF;
				entries[i].fragmentsRecieved = 0;
				entries[i].numFragments = 0;
				entries[i].fragments = null;
			}
		}
		latestSequence = seq;
	}
	
	public void processFragment(int seq, int fragmentId, int numFragments, ByteBuffer data, MessageContext c){
		if(data.remaining() <= 0 ||
				numFragments > 256 ||
				numFragments < 0 ||
				fragmentId < 0 ||
				fragmentId >= numFragments ||
				UDPNetworkManager.sequenceDifference(seq, latestSequence) > 1024 ||
				(fragmentId != (numFragments-1) && data.remaining() != UDPNetworkManager.MAX_PACKET_SIZE) ||
				data.remaining() > UDPNetworkManager.MAX_PACKET_SIZE)
			return;
		int idx = seq % maxEntries;
		if(sequence[idx] == 0xFFFFFFFF){
			advance(seq);
			entries[idx].fragments = new byte[numFragments][];
			entries[idx].numFragments = numFragments;
			sequence[idx] = seq;
			addFragmentData(entries[idx], fragmentId, data, c);
		} else if(sequence[idx] == seq){
			addFragmentData(entries[idx], fragmentId, data, c);
		}
	}
	
	public void addFragmentData(Entry e, int fragId, ByteBuffer data, MessageContext c){
		e.fragments[fragId] = new byte[data.remaining()];
		byte[] arr = data.array();
		System.arraycopy(arr, data.position(), e.fragments[fragId], 0, data.remaining());
		//data.get(e.fragments[fragId]);
		e.fragmentsRecieved ++;
		if(e.fragmentsRecieved == e.numFragments){
			reconstructAndRecievePacket(e, data, c);
		}
	}
	
	public void reconstructAndRecievePacket(Entry e, ByteBuffer data, MessageContext c){
		data.clear();
		byte[] arr = data.array();
		for(byte[] b : e.fragments){
			System.arraycopy(b, 0, arr, data.position(), b.length);
			data.position(data.position()+b.length);
			//data.put(b);
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
	
	public void recieveFragmentPacket(ByteBuffer buf, MessageContext c) {
		int seq = buf.getShort() & 0xFFFF;
		int fragId = buf.get() & 0xFF;
		int numFragments = buf.get() & 0xFF;
		processFragment(seq, fragId, numFragments, buf, c);
	}
	
	public void fragmentAndSendPacket(UDPConnection c, ByteBuffer buf) throws IOException {
		int limit = buf.limit();
		short seq = unreliableFragmentSequenceNumber ++;
		int fragId = 0;
		int fragments = (int) Math.ceil((float)buf.remaining()/1024);
		while(buf.remaining() > 0){
			int amount = Math.min(buf.remaining(), UDPNetworkManager.MAX_PACKET_SIZE);
			buf.limit(buf.position()+amount);
			buf.position(buf.position()-5);
			buf.put((byte) UDPNetworkManager.PacketType.FRAGMENT.ordinal());
			buf.putShort(seq);
			buf.put((byte) fragId);
			buf.put((byte)fragments);
			int oldPos = buf.position()-5;
			buf.position(oldPos);
			
			CRC32 crc = new CRC32();
			UDPNetworkManager.addMagicProtocolID(crc);
			crc.update(buf);
			buf.position(oldPos-4);
			buf.putInt((int) (crc.getValue()&0xFFFFFFFF));
			buf.position(oldPos-4);
			if(c.channel.isConnected()){
				c.channel.write(buf);
			} else {
				c.channel.send(buf, c.remoteAddress);
			}
			
			buf.limit(limit);
			fragId ++;
		}
	}
	
	public static class Entry {
		int numFragments;
		int fragmentsRecieved = 0;
		byte[][] fragments;
	}

}
