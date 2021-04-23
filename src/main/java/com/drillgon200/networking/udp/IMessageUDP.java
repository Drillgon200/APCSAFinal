package com.drillgon200.networking.udp;

public interface IMessageUDP {

	public boolean reliable();
	
	public void serialize(Stream s);
}
