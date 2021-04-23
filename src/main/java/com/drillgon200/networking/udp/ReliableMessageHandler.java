package com.drillgon200.networking.udp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.zip.CRC32;

import com.drillgon200.networking.udp.UDPNetworkManager.PacketInfo;
import com.drillgon200.shooter.Side;

public class ReliableMessageHandler {

	public static final int BUFFER_SIZE = 1024;
	public static final double RESEND_DELAY = 0.02;
	public static final int MAX_MESSAGES_PER_PACKET = 64;
	
	public UDPConnection connection;
	public DatagramChannel channel;
	
	byte[] chunkToSend = null;
	public ChunkSender chunkSender;
	public ChunkReceiver chunkReceiver;
	
	public double timeAccumulator = 0;
	
	//Sent message data
	public double lastAckSendTime = -1;
	public short currentMessageId = 0;
	public short oldestMessageId = 0;
	public int[] messageSequenceBuffer = new int[BUFFER_SIZE];
	public IMessageUDP[] toSend = new IMessageUDP[BUFFER_SIZE];
	public int[] messageSizes = new int[BUFFER_SIZE];
	public boolean[] messageAcks = new boolean[BUFFER_SIZE];
	public double[] messageLastSentTimes = new double[BUFFER_SIZE];
	
	//Sent packet data
	public short packetSequence = 0;
	public boolean[] sentPacketAcks = new boolean[BUFFER_SIZE];
	public int[] sentPackets = new int[BUFFER_SIZE];
	public byte[] sentPacketMessageCounts = new byte[BUFFER_SIZE];
	public short[] sentPacketMessageIds = new short[MAX_MESSAGES_PER_PACKET*BUFFER_SIZE];
	
	//Received message data
	public short receiveMessageId = 0;
	public IMessageUDP[] receiveBuffer = new IMessageUDP[BUFFER_SIZE];
	public byte[] messageIds = new byte[BUFFER_SIZE];
	
	//Received packet data
	public short receivePacketSequence = 0;
	public int[] receivedPackets = new int[BUFFER_SIZE];
	
	public ReliableMessageHandler(UDPConnection c) {
		chunkSender = new ChunkSender(c);
		chunkReceiver = new ChunkReceiver(c);
		this.channel = c.channel;
		this.connection = c;
		for(int i = 0; i < BUFFER_SIZE; i ++){
			messageSequenceBuffer[i] = 0xFFFFFFFF;
			receivedPackets[i] = 0xFFFFFFFF;
			sentPackets[i] = 0xFFFFFFFF;
			messageAcks[i] = false;
			sentPacketAcks[i] = false;
		}
	}
	
	public boolean acceptMessage(ByteBuffer buf, IMessageUDP message) throws IOException {
		if(UDPNetworkManager.sequenceDifference(currentMessageId, oldestMessageId) >= 1024 || chunkToSend != null || chunkSender.sending)
			return false;
		MeasureStream stream = new MeasureStream();
		message.serialize(stream);
		int bits = stream.getBits() + 8;
		if(bits > UDPNetworkManager.MAX_PACKET_SIZE*8){
			WriteStream writeStream = new WriteStream(new BitWriter(buf));
			buf.position(9);
			int id = UDPNetworkManager.idsByMessage.get(message.getClass());
			writeStream.serializeBits(id, 8);
			message.serialize(writeStream);
			writeStream.finish();
			buf.flip();
			
			buf.position(9);
			chunkToSend = new byte[buf.remaining()];
			buf.get(chunkToSend);
		} else {
			int seq = currentMessageId++;
			seq = seq & 0xFFFF;
			int idx = seq % BUFFER_SIZE;
			messageLastSentTimes[idx] = -1;
			messageAcks[idx] = false;
			messageSequenceBuffer[idx] = seq;
			messageSizes[idx] = stream.getBits() + 8; //Packet ids are stored in 8 bits.
			toSend[idx] = message;
		}
		return true;
	}
	
	public boolean receiveMessage(MessageContext c){
		int idx = receiveMessageId % BUFFER_SIZE;
		if(receiveBuffer[idx] == null)
			return false;
		int id = messageIds[idx] & 0xFF;
		UDPNetworkManager.packets.get(id).onMessage(receiveBuffer[idx], c);
		receiveBuffer[idx] = null;
		receiveMessageId ++;
		return true;
	}
	
	public void processMessageAck(short ack){
		int idx = (ack & 0xFFFF) % BUFFER_SIZE;
		int count = sentPacketMessageCounts[idx];
		for(int i = 0; i < count; i ++){
			short messageId = sentPacketMessageIds[idx*MAX_MESSAGES_PER_PACKET+i];
			int mIdx = (messageId & 0xFFFF) % BUFFER_SIZE;
			if(messageSequenceBuffer[mIdx] == messageId){
				messageAcks[mIdx] = true;
				//System.err.println("Acking message with index " + mIdx);
			}
		}
	}
	
	public void processAcks(short ack, int ackBits){
		for(int i = 0; i < 32; i ++){
			if((ackBits & 1) > 0){
				short seq = (short) (ack - i);
				int idx = (seq & 0xFFFF) % BUFFER_SIZE;
				if(sentPackets[idx] == seq && !sentPacketAcks[idx]){
					sentPacketAcks[idx] = true;
					sentPackets[idx] = 0xFFFFFFFF;
					processMessageAck(seq);
				}
			}
			ackBits = ackBits >>> 1;
		}
		removeOldAckedMessages();
	}
	
	public void readPacket(ByteBuffer buf, MessageContext c){
		short numMessages = buf.getShort();
		ReadStream stream = new ReadStream(new BitReader(buf));
		short seq = stream.serializeShort((short) 0);
		if(UDPNetworkManager.sequenceGreaterThan(seq, receivePacketSequence)){
			for(int i = 0; i < UDPNetworkManager.sequenceDifference(seq, receivePacketSequence); i ++){
				int idx = ((seq&0xFFFF)+i) % BUFFER_SIZE;
				receivedPackets[idx] = 0xFFFFFFFF;
			}
			int idx = (seq&0xFFFF) % BUFFER_SIZE;
			receivedPackets[idx] = seq;
			receivePacketSequence = seq;
		}
		short ack = stream.serializeShort((short) 0);
		int ackBits = stream.serializeInt(0);
		processAcks(ack, ackBits);
		/*if(numMessages > 0){
			System.out.println("Read buffer position is " + buf.position());
			System.out.println("Read data " + buf.get(15) + " " + buf.get(16) + " " + buf.get(17));
		}*/
		for(int i = 0; i < numMessages; i ++){
			short messageId = stream.serializeShort((short) 0);
			int mIdx = (messageId & 0xFFFF) % BUFFER_SIZE;
			int registerId = stream.serializeBits(0, 8);
			//System.out.println("Receiving packet with index " + mIdx + " and registerId " + registerId);
			try {
				PacketInfo<? extends IMessageUDP> info = UDPNetworkManager.packets.get(registerId);
				if(info == null){
					throw new PacketException("Unregistered packet: " + registerId);
				}
				IMessageUDP message = info.newMessage();
				message.serialize(stream);
				receiveBuffer[mIdx] = message;
				messageIds[mIdx] = (byte) registerId;
			} catch(PacketException e){
				e.printStackTrace();
			}
		}
		
		while(receiveMessage(c));
	}
	
	public void writePacket(ByteBuffer buf) throws IOException {
		int ackBits = genAckBits();
		int diff = UDPNetworkManager.sequenceDifference(currentMessageId, oldestMessageId);
		if((diff == 0 && ackBits == 0) || timeAccumulator - lastAckSendTime < RESEND_DELAY){
			return;
		}
		lastAckSendTime = timeAccumulator;
		int sent = 0;
		int sentBits = 16 + 16 + 32; //Sequence, ack, ack bits
		int maxSendBits = UDPNetworkManager.MAX_PACKET_SIZE*8;
		buf.position(9);
		WriteStream stream = new WriteStream(new BitWriter(buf));
		short seq = packetSequence++;
		stream.serializeShort(seq);
		stream.serializeShort((short) (receivePacketSequence-1));
		stream.serializeInt(ackBits);
		
		int idx = (seq&0xFFFF) % BUFFER_SIZE;
		sentPackets[idx] = seq;
		sentPacketAcks[idx] = false;
		
		short currentMessageId = oldestMessageId;
		/*if(diff > 0){
			System.out.println("Write buffer position is " + buf.position());
			short ack2 =(short) (receivePacketSequence-1);
			System.out.println("Prev numbers " + seq + " " + ack2 + " " + ackBits);
		}*/
		for(short i = 0; i < diff; i ++){
			int idx2 = currentMessageId % BUFFER_SIZE;
			if(!messageAcks[idx2] && timeAccumulator - messageLastSentTimes[idx2] > RESEND_DELAY){
				if(messageSizes[idx2] + sentBits <= maxSendBits){
					sentPacketMessageIds[idx*MAX_MESSAGES_PER_PACKET+sent] = currentMessageId;
					sent ++;
					sentBits += messageSizes[idx2];
					int id = UDPNetworkManager.idsByMessage.get(toSend[idx2].getClass());
					//System.out.println("Serializing message " + currentMessageId + " " + id + " " + stream.writer.bitCount + " " + stream.writer.scratch);
					stream.serializeShort(currentMessageId);
					stream.serializeBits(id, 8);
					toSend[idx2].serialize(stream);
					messageLastSentTimes[idx2] = timeAccumulator;
					if(sent == MAX_MESSAGES_PER_PACKET)
						break;
				} else {
					break;
				}
			}
			currentMessageId += 1;
		}
		stream.finish();
		/*if(diff > 0){
			System.out.println("Write data " + buf.get(17) + " " + buf.get(18) + " " + buf.get(19));
		}*/
		buf.put(6, (byte) UDPNetworkManager.PacketType.RELIABLE_MESSAGE.ordinal());
		buf.putShort(7, (short) sent);
		sentPacketMessageCounts[idx] = (byte) sent;
		buf.flip();
		buf.position(6);
		CRC32 crc = new CRC32();
		UDPNetworkManager.addMagicProtocolID(crc);
		crc.update(buf);
		buf.position(2);
		buf.putInt((int) (crc.getValue()&0xFFFFFFFF));
		buf.position(2);
		if(channel.isConnected()){
			channel.write(buf);
		} else {
			channel.send(buf, connection.remoteAddress);
		}
		buf.clear();
	}
	
	private int genAckBits() {
		short ack = (short) (receivePacketSequence-1);
		int ackBits = 0;
		for(int i = 0; i < 32; i ++){
			short seq = (short) (ack - i);
			int idx = (seq & 0xFFFF) % BUFFER_SIZE;
			if(receivedPackets[idx] == (seq & 0xFFFF)){
				ackBits |= (1 << i);
			}
		}
		return ackBits;
	}

	public void removeOldAckedMessages(){
		int diff = UDPNetworkManager.sequenceDifference(currentMessageId, oldestMessageId);
		int removed = 0;
		for(int i = 0; i < diff; i ++){
			int idx = ((oldestMessageId & 0xFFFF) + i) % BUFFER_SIZE;
			if(!messageAcks[idx])
				break;
			removed ++;
			messageSequenceBuffer[idx] = 0xFFFFFFFF;
			toSend[idx] = null;
		}
		oldestMessageId += removed;
	}
	
	public void clearOldData(){
		
	}
	
	public void update(ByteBuffer buf, double dt) throws IOException {
		timeAccumulator += dt;
		if(chunkToSend != null && oldestMessageId == currentMessageId){
			buf.position(9);
			buf.put(chunkToSend);
			buf.flip();
			buf.position(9);
			chunkSender.sendChunk(buf);
			chunkToSend = null;
		}
		chunkSender.update(buf, dt);
		chunkReceiver.update(dt);
		writePacket(buf);
		clearOldData();
	}
}
