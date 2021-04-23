package com.drillgon200.networking.udp;

public interface IMessageHandlerUDP<MSG extends IMessageUDP> {

	public void onMessage(MSG m, MessageContext c);
}
