package com.drillgon200.networking.tcp;

public interface IMessageHandlerTCP<T extends IMessageTCP> {

	public void onMessage(T m, TCPConnection c);
}
