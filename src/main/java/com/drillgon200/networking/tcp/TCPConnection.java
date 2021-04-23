package com.drillgon200.networking.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPConnection {

	public SocketChannel channel;
	public TCPMessageReader reader;
	public TCPMessageWriter writer;
	public volatile boolean isClosed = false;
	public long lastCommunicatedTime;
	
	public TCPConnection(SocketChannel c) {
		this.channel = c;
		reader = new TCPMessageReader();
		writer = new TCPMessageWriter();
	}
	
	public int read(ByteBuffer buf) throws IOException {
		int bytesRead = channel.read(buf);
		int totalBytesRead = bytesRead;
		while(bytesRead > 0){
			bytesRead = channel.read(buf);
			totalBytesRead += bytesRead;
		}
		if(bytesRead == -1)
			isClosed = true;
		return totalBytesRead;
	}
	
	public int write(ByteBuffer buf) throws IOException {
		int bytesWritten = channel.write(buf);
		int totalBytesWritten = bytesWritten;
		while(bytesWritten > 0 && buf.hasRemaining()){
			bytesWritten = channel.write(buf);
			totalBytesWritten += bytesWritten;
		}
		return totalBytesWritten;
	}
	
	public void sendPacket(IMessageTCP m){
		if(TCPNetworkManager.idsByMessage.containsKey(m.getClass())){
			writer.writeQueue.add(m);
		} else {
			throw new RuntimeException("Unregistered packet: " + m.getClass());
		}
	}
}
