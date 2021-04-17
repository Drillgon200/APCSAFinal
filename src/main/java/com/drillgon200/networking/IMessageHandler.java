package com.drillgon200.networking;

public interface IMessageHandler<T extends IMessage> {

	public void onMessage(T m, Connection c);
}
