package com.drillgon200.networking.udp;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.drillgon200.shooter.Side;
import com.drillgon200.shooter.entity.Player;

public class UDPConnection {

	public SocketAddress remoteAddress;
	public DatagramChannel channel;
	public Side side;
	public Player player;
	public volatile boolean isClosed = false;
	public FragmentHandler fragHandler;
	
	public ReliableMessageHandler reliableMessageHandler;
	public Queue<IMessageUDP> messages = new LinkedBlockingQueue<>();
	public Queue<IMessageUDP> reliableMessages = new LinkedBlockingQueue<>();
	public long lastCommunicatedTime;
	
	public UDPConnection(DatagramChannel c, SocketAddress address, Side side) {
		channel = c;
		this.remoteAddress = address;
		fragHandler = new FragmentHandler();
		reliableMessageHandler = new ReliableMessageHandler(this);
		this.side = side;
	}

	public void sendMessage(IMessageUDP m) {
		if(m.reliable()){
			reliableMessages.add(m);
		} else {
			messages.add(m);
		}
	}
	
	@Override
	public String toString() {
		return "UDP connection with remote address: " + remoteAddress;
	}
}
