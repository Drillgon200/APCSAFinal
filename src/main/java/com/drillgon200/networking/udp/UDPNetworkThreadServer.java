package com.drillgon200.networking.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;

import com.drillgon200.networking.udp.UDPNetworkManager.PacketInfo;
import com.drillgon200.networking.udp.UDPNetworkManager.PacketType;
import com.drillgon200.shooter.Side;

public class UDPNetworkThreadServer extends Thread {
	
	public volatile boolean shouldShutDown = false;
	public DatagramChannel channel;
	public DatagramChannel localChannel;
	public List<UDPConnection> activeConnections = new ArrayList<>();
	public ByteBuffer ioBuffer;
	
	public ReadStream readStream;
	public WriteStream writeStream;
	
	long prevTime;
	long currentTime;
	public int port;
	
	public Deque<String> log;
	
	public UDPNetworkThreadServer(int port) {
		this(port, null);
	}
	
	public UDPNetworkThreadServer(int port, Deque<String> log) {
		this.port = port;
		this.log = log;
		this.setName("Server Network Thread");
	}
	
	public void terminate(){
		shouldShutDown = true;
	}
	
	public void setLog(Deque<String> log){
		this.log = log;
	}
	
	private void init() throws IOException {
		channel = DatagramChannel.open();
		channel.setOption(StandardSocketOptions.SO_SNDBUF, 1024*512);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*512);
		channel.configureBlocking(false);
		InetSocketAddress addr = new InetSocketAddress(getLocalAddress(), port);
		channel.socket().bind(addr);
		
		localChannel = DatagramChannel.open();
		localChannel.setOption(StandardSocketOptions.SO_SNDBUF, 1024*512);
		localChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*512);
		localChannel.configureBlocking(false);
		addr = new InetSocketAddress(InetAddress.getLoopbackAddress().getHostAddress(), port);
		localChannel.socket().bind(addr);
		
		ioBuffer = ByteBuffer.allocate(1024*1024);
		ioBuffer.order(ByteOrder.LITTLE_ENDIAN);
		readStream = new ReadStream(new BitReader(ioBuffer));
		writeStream = new WriteStream(new BitWriter(ioBuffer));
		prevTime = currentTime = System.currentTimeMillis();
	}
	
	private String getLocalAddress(){
		String addr = "";
		try {
			addr = InetAddress.getLocalHost().getHostAddress();
		} catch(UnknownHostException e1) {}
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			addr = socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch(SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
		return addr;
	}
	
	@Override
	public void run() {
		try {
			init();
		} catch(IOException e1) {
			e1.printStackTrace();
		}
		while(!shouldShutDown){
			try {
				prevTime = currentTime;
				currentTime = System.currentTimeMillis();
				double dt = (currentTime-prevTime)/1000D;
				for(UDPConnection connection : activeConnections){
					connection.reliableMessageHandler.update(ioBuffer, dt);
				}
				read();
				writeConnections();
				Iterator<UDPConnection> itr = activeConnections.iterator();
				while(itr.hasNext()){
					UDPConnection connection = itr.next();
					if(currentTime - connection.lastCommunicatedTime > UDPNetworkManager.TIMEOUT){
						System.out.println("Connection timed out: " + connection);
						if(log != null){
							log.addLast("Connection timed out: " + connection);
						}
						itr.remove();
						connection.isClosed = true;
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		try {
			channel.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		try {
			localChannel.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void read() throws IOException {
		SocketAddress addr = null;
		long time = System.currentTimeMillis();
		while((addr = channel.receive(ioBuffer)) != null){
			receiveDataPacket(channel, addr, time);
		}
		while((addr = localChannel.receive(ioBuffer)) != null){
			receiveDataPacket(localChannel, addr, time);
		}
	}
	
	private void receiveDataPacket(DatagramChannel channel, SocketAddress addr, long time){
		/*if(Math.random() < 0.8){
			ioBuffer.clear();
			System.out.println("dropping packet");
			continue;
		}*/
		UDPConnection connection = null;
		boolean newConnection = false;
		connection = getConnection(addr);
		ioBuffer.flip();
		int packetCRC = ioBuffer.getInt();
		CRC32 crc = new CRC32();
		UDPNetworkManager.addMagicProtocolID(crc);
		crc.update(ioBuffer);
		if((int)(crc.getValue()&0xFFFFFFFF) != packetCRC){
			System.err.println("Server packet checksum doesn't match!");
			ioBuffer.clear();
			return;
		}
		if(connection == null){
			connection = new UDPConnection(channel, addr, Side.SERVER);
			newConnection = true;
			System.out.println("Accepting connection: " + connection);
			if(log != null)
				log.addLast("Accepting connection: " + connection);
		}
		ioBuffer.position(4);
		PacketType type = PacketType.values()[ioBuffer.get()];
		switch(type){
		case FRAGMENT:
			connection.fragHandler.recieveFragmentPacket(ioBuffer, new MessageContext(Side.SERVER, connection));
			break;
		case RELIABLE_FRAGMENT:
			connection.reliableMessageHandler.chunkReceiver.receiveSlicePacket(ioBuffer, new MessageContext(Side.SERVER, connection));
			break;
		case MESSAGE:
			int id = readStream.serializeBits(0, 8);
			try {
				PacketInfo<? extends IMessageUDP> info = UDPNetworkManager.packets.get(id);
				if(info == null){
					throw new PacketException("Unregistered packet: " + id);
				}
				info.handleMessage(readStream, new MessageContext(Side.SERVER, connection));
			} catch(PacketException e){
				e.printStackTrace();
				ioBuffer.clear();
				return;
			}
			break;
		case RELIABLE_MESSAGE:
			connection.reliableMessageHandler.readPacket(ioBuffer, new MessageContext(Side.CLIENT, connection));
			break;
		}
		if(newConnection){
			activeConnections.add(connection);
		}
		connection.lastCommunicatedTime = time;
		readStream.clear();
		ioBuffer.clear();
	}
	
	private void writeConnections() throws IOException {
		for(UDPConnection c : activeConnections){
			write(c);
		}
	}
	
	private void write(UDPConnection connection) throws IOException {
		Iterator<IMessageUDP> itr = connection.messages.iterator();
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
				connection.fragHandler.fragmentAndSendPacket(connection, ioBuffer);
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
				connection.channel.send(ioBuffer, connection.remoteAddress);
				ioBuffer.clear();
				writeStream.clear();
				itr.remove();
			}
		}
		itr = connection.reliableMessages.iterator();
		while(itr.hasNext()){
			IMessageUDP m = itr.next();
			if(!connection.reliableMessageHandler.acceptMessage(ioBuffer, m)){
				ioBuffer.clear();
				break;
			}
			ioBuffer.clear();
			itr.remove();
		}
		/*if(!connection.chunkSender.sending){
			itr = connection.reliableMessages.iterator();
			while(itr.hasNext()){
				IMessageUDP m = itr.next();
				ioBuffer.position(9);
				writeStream.serializeBits(UDPNetworkManager.idsByMessage.get(m.getClass()), 8);
				m.serialize(writeStream);
				writeStream.finish();
				ioBuffer.flip();
				
				ioBuffer.position(9);
				int size = ioBuffer.remaining();
				if(size > UDPNetworkManager.MAX_PACKET_SIZE){
					connection.chunkSender.sendChunk(ioBuffer);
					ioBuffer.clear();
					itr.remove();
					break;
				} else {
					if(!connection.reliableMessageHandler.acceptMessage(m)){
						ioBuffer.clear();
						break;
					}
				}
				ioBuffer.clear();
				itr.remove();
			}
		}*/
	}
	
	public UDPConnection getConnection(SocketAddress s){
		for(UDPConnection c : activeConnections){
			if(c.remoteAddress.equals(s)){
				return c;
			}
		}
		return null;
	}
	
}
