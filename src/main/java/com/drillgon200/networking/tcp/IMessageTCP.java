package com.drillgon200.networking.tcp;

import java.nio.ByteBuffer;

public interface IMessageTCP {
	public void write(ByteBuffer buffer);
	public void read(ByteBuffer buffer);
}
