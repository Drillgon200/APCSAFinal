package com.drillgon200.networking.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPMessageWriter {
	
	//Creating a 1 mb byte buffer for each connection shouldn't matter that much since we're only making like 20 connections max
	public ByteBuffer buf = ByteBuffer.allocate(1024*1024);
	public Queue<IMessageTCP> writeQueue = new LinkedBlockingQueue<>();
	public IMessageTCP currentMessage = null;
	public int bytesWritten = 0;
	
	public void write(TCPConnection c) throws IOException {
		while(!writeQueue.isEmpty() || currentMessage != null){
			if(currentMessage == null){
				currentMessage = writeQueue.poll();
				buf.rewind();
				//Integers for length and id at the beginning
				buf.position(4*2);
				currentMessage.write(buf);
				buf.limit(buf.position());
				int size = buf.position()-4*2;
				buf.position(0);
				buf.putInt(size);
				buf.putInt(TCPNetworkManager.idsByMessage.get(currentMessage.getClass()));
				buf.position(0);
			}
			bytesWritten += c.write(buf);
			if(bytesWritten >= buf.limit()){
				currentMessage = null;
				buf.clear();
				bytesWritten = 0;
			} else {
				break;
			}
		}
	}
}
