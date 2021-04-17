package com.drillgon200.networking;

import java.nio.ByteBuffer;

public interface IMessage {
	public void write(ByteBuffer buffer);
	public void read(ByteBuffer buffer);
}
