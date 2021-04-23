package com.drillgon200.networking.udp;

import java.nio.ByteBuffer;

import com.drillgon200.shooter.Side;

public class MessageContext {

	public Side side;
	public ByteBuffer messageBuffer;
	public UDPConnection connection;
	
	public MessageContext(Side side, UDPConnection connection) {
		this.side = side;
		this.connection = connection;
	}
}
