package com.drillgon200.networking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection {

	public SocketChannel channel;
	public MessageReader reader;
	public MessageWriter writer;
	public volatile boolean isClosed = false;
	public long lastCommunicatedTime;
	
	public Connection(SocketChannel c) {
		this.channel = c;
		reader = new MessageReader();
		writer = new MessageWriter();
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
	
	public void sendPacket(IMessage m){
		if(NetworkManager.idsByMessage.containsKey(m.getClass())){
			writer.writeQueue.add(m);
		} else {
			throw new RuntimeException("Unregistered packet: " + m.getClass());
		}
	}
}
