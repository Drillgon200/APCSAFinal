package com.drillgon200.networking.udp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.zip.CRC32;

import com.drillgon200.networking.udp.UDPNetworkManager.PacketInfo;
import com.drillgon200.networking.udp.UDPNetworkManager.PacketType;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.Side;

public class UDPNetworkThreadClient extends Thread {

	public volatile boolean shouldShutDown = false;
	public InetAddress server;
	public int port;
	public DatagramChannel channel;
	public volatile UDPConnection serverConnection;
	public ByteBuffer ioBuffer;
	
	public ReadStream readStream;
	public WriteStream writeStream;
	
	long prevTime;
	long currentTime;
	
	public Deque<String> log;
	
	public UDPNetworkThreadClient() {
		this.setName("Client Network Thread");
	}
	
	public void setLog(Deque<String> log){
		this.log = log;
	}
	
	public void terminate(){
		shouldShutDown = true;
	}
	
	private void init(){
		ioBuffer = ByteBuffer.allocate(1024*1024);
		ioBuffer.order(ByteOrder.LITTLE_ENDIAN);
		readStream = new ReadStream(new BitReader(ioBuffer));
		writeStream = new WriteStream(new BitWriter(ioBuffer));
		prevTime = currentTime = System.currentTimeMillis();
	}
	
	@Override
	public void run() {
		try {
			init();
			while(!shouldShutDown){
				//I don't know if it's the best practice to do this, but it makes it not use 20% of my CPU and works I guess?
				try {
					Thread.sleep(1);
				} catch(InterruptedException e1) {
					e1.printStackTrace();
				}
				prevTime = currentTime;
				currentTime = System.currentTimeMillis();
				if(serverConnection != null){
					double dt = (currentTime-prevTime)/1000D;
					serverConnection.reliableMessageHandler.update(ioBuffer, dt);
					if(currentTime - serverConnection.lastCommunicatedTime > UDPNetworkManager.TIMEOUT){
						Shooter.state = ConnectionState.DISCONNECTING;
						serverConnection.isClosed = true;
						serverConnection.channel.close();
						serverConnection = null;
						System.out.println("Server timed out.");
						if(log != null){
							log.addLast("Server timed out.");
						}
						continue;
					}
					if(serverConnection.isClosed){
						serverConnection.channel.close();
						serverConnection = null;
						System.out.println("Disconnecting from server.");
						if(log != null){
							log.addLast("Disconnecting from server.");
						}
					}
					readConnection();
					if(serverConnection != null)
						writeConnection();
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void readConnection() throws IOException, PacketException {
		boolean didRead = false;
		SocketAddress addr = null;
		while((addr = channel.receive(ioBuffer)) != null){
			ioBuffer.flip();
			int packetCRC = ioBuffer.getInt();
			CRC32 crc = new CRC32();
			UDPNetworkManager.addMagicProtocolID(crc);
			crc.update(ioBuffer);
			if((int)(crc.getValue()&0xFFFFFFFF) != packetCRC){
				System.err.println("Client packet checksum doesn't match!");
				ioBuffer.clear();
				continue;
			}
			ioBuffer.position(4);
			PacketType type = PacketType.values()[ioBuffer.get()];
			switch(type){
			case FRAGMENT:
				serverConnection.fragHandler.recieveFragmentPacket(ioBuffer, new MessageContext(Side.CLIENT, serverConnection));
				break;
			case RELIABLE_FRAGMENT:
				serverConnection.reliableMessageHandler.chunkReceiver.receiveSlicePacket(ioBuffer, new MessageContext(Side.CLIENT, serverConnection));
				break;
			case MESSAGE:
				int id = readStream.serializeBits(0, 8);
				try {
					PacketInfo<? extends IMessageUDP> info = UDPNetworkManager.packets.get(id);
					if(info == null){
						throw new PacketException("Unregistered packet: " + id);
					}
					info.handleMessage(readStream, new MessageContext(Side.CLIENT, serverConnection));
				} catch(PacketException e){
					e.printStackTrace();
					ioBuffer.clear();
					continue;
				}
				break;
			case RELIABLE_MESSAGE:
				serverConnection.reliableMessageHandler.readPacket(ioBuffer, new MessageContext(Side.CLIENT, serverConnection));
				break;
			}
			didRead = true;
			readStream.clear();
			ioBuffer.clear();
		}
		if(didRead){
			serverConnection.lastCommunicatedTime = System.currentTimeMillis();
		}
	}
	
	private void writeConnection() throws IOException {
		Iterator<IMessageUDP> itr = serverConnection.messages.iterator();
		while(itr.hasNext()){
			IMessageUDP m = itr.next();
			ioBuffer.position(9);
			writeStream.serializeBits(UDPNetworkManager.idsByMessage.get(m.getClass()), 8);
			m.serialize(writeStream);
			writeStream.finish();
			ioBuffer.flip();
			
			//Position 9 because that's the biggest size of a header I have right now, and the header is determined after
			//I figure out how big it is.
			ioBuffer.position(9);
			int size = ioBuffer.remaining();
			if(size > UDPNetworkManager.MAX_PACKET_SIZE){
				serverConnection.fragHandler.fragmentAndSendPacket(serverConnection, ioBuffer);
			} else {
				//Construct header
				ioBuffer.position(8);
				ioBuffer.put(8, (byte) UDPNetworkManager.PacketType.MESSAGE.ordinal());
				//IP already has a checksum, but the article I'm reading says it's important to implement your
				//own as well since the IP one is only 16 bits, and packet corruption does happen.
				CRC32 crc = new CRC32();
				UDPNetworkManager.addMagicProtocolID(crc);
				crc.update(ioBuffer);
				int val = (int) (crc.getValue()&0xFFFFFFFF);
				ioBuffer.putInt(4, val);
				ioBuffer.position(4);
				
				//If it returns 0, just drop it.
				channel.write(ioBuffer);
			}
			ioBuffer.clear();
			itr.remove();
		}
		itr = serverConnection.reliableMessages.iterator();
		while(itr.hasNext()){
			IMessageUDP m = itr.next();
			if(!serverConnection.reliableMessageHandler.acceptMessage(ioBuffer, m)){
				ioBuffer.clear();
				break;
			}
			ioBuffer.clear();
			itr.remove();
		}
		/*if(!serverConnection.reliableMessageHandler.chunkSender.sending){
			itr = serverConnection.reliableMessages.iterator();
			while(itr.hasNext()){
				IMessageUDP m = itr.next();
				ioBuffer.position(9);
				int id = UDPNetworkManager.idsByMessage.get(m.getClass());
				writeStream.serializeBits(id, 8);
				m.serialize(writeStream);
				writeStream.finish();
				ioBuffer.flip();
				
				ioBuffer.position(9);
				int size = ioBuffer.remaining();
				if(size > UDPNetworkManager.MAX_PACKET_SIZE){
					serverConnection.reliableMessageHandler.chunkSender.sendChunk(ioBuffer);
					ioBuffer.clear();
					itr.remove();
					break;
				} else {
					if(!serverConnection.reliableMessageHandler.acceptMessage(ioBuffer, m)){
						ioBuffer.clear();
						break;
					}
				}
				ioBuffer.clear();
				itr.remove();
			}
		}*/
	}
	
	public void connect(InetAddress server, int port) throws IOException {
		//Block until initialization is complete
		close();
		channel = DatagramChannel.open();
		channel.setOption(StandardSocketOptions.SO_SNDBUF, 1024*512);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*512);
		SocketAddress addr = new InetSocketAddress(0);
		channel.socket().bind(addr);
		addr = new InetSocketAddress(server, port);
		channel.connect(addr);
		UDPConnection c = new UDPConnection(channel, addr, Side.CLIENT);
		channel.configureBlocking(false);
		this.server = server;
		this.port = port;
		serverConnection = c;
		serverConnection.lastCommunicatedTime = System.currentTimeMillis();
	}
	
	public void close(){
		if(serverConnection == null)
			return;
		serverConnection.isClosed = true;
		try {
			serverConnection.channel.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		serverConnection = null;
	}
	
	public void sendMessage(IMessageUDP m){
		if(serverConnection != null){
			serverConnection.sendMessage(m);
		}
	}
}
