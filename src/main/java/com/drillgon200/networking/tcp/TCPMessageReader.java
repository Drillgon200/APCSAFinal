package com.drillgon200.networking.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.drillgon200.networking.tcp.TCPNetworkManager.PacketInfo;

public class TCPMessageReader {

	public ByteBuffer buf = ByteBuffer.allocate(1024*1024);
	
	public int read(TCPConnection c) throws IOException {
		int amountRead = c.read(buf);
		while(buf.position() >= 2*4){
			int size = buf.getInt(0);
			int id = buf.getInt(4);
			if(buf.position() >= 2*4+size){
				buf.flip();
				PacketInfo<? extends IMessageTCP> info = TCPNetworkManager.packets.get(id);
				if(info == null){
					throw new RuntimeException("Unregistered packet: " + id);
				}
				buf.position(buf.position()+2*4);
				info.handleMessage(buf, c);
				buf.compact();
			}
		}
		return amountRead;
	}
}
