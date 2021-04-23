package com.drillgon200.networking.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TCPNetworkThreadServer extends Thread {
	
	public volatile boolean shouldShutDown = false;
	public Queue<TCPConnection> inboundConnections;
	public List<TCPConnection> activeConnections = new ArrayList<>();
	public Selector readSelector;
	public Selector writeSelector;
	long currentTime;
	
	public Deque<String> log;
	
	public TCPNetworkThreadServer(Queue<TCPConnection> inboundConnections) throws IOException {
		this.setName("Server Network Thread");
		this.inboundConnections = inboundConnections;
		readSelector = Selector.open();
		writeSelector = Selector.open();
	}
	
	public void terminate(){
		shouldShutDown = true;
	}
	
	public void setLog(Deque<String> log){
		this.log = log;
	}
	
	@Override
	public void run() {
		init();
		while(!shouldShutDown){
			//I don't know if it's the best practice to do this, but it makes it not use 20% of my CPU and works I guess?
			try {
				Thread.sleep(1);
			} catch(InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				currentTime = System.currentTimeMillis();
				acceptNewConnections();
				readFromConnections();
				writeToConnections();
				Iterator<TCPConnection> itr = activeConnections.iterator();
				while(itr.hasNext()){
					TCPConnection c = itr.next();
					if(currentTime - c.lastCommunicatedTime > TCPNetworkManager.TIMEOUT || !c.channel.isConnected()){
						itr.remove();
						c.isClosed = true;
						try {
							c.channel.close();
						} catch(IOException e) {
							e.printStackTrace();
						}
						System.out.println("Connection timed out: " + c.channel);
						if(log != null)
							log.addLast("Connection timed out: " + c.channel);
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		closeAll();
	}
	
	public void closeConnection(TCPConnection c){
		c.isClosed = true;
		try {
			c.channel.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		activeConnections.remove(c);
	}
	
	public void closeAll(){
		for(TCPConnection c : activeConnections){
			c.isClosed = true;
			try {
				c.channel.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		activeConnections.clear();
	}
	
	private void acceptNewConnections() throws IOException {
		TCPConnection inSocket;
		while((inSocket = inboundConnections.poll()) != null){
			inSocket.channel.configureBlocking(false);
			
			SelectionKey key = inSocket.channel.register(readSelector, SelectionKey.OP_READ);
			key.attach(inSocket);
			key = inSocket.channel.register(writeSelector, SelectionKey.OP_WRITE);
			key.attach(inSocket);
			
			activeConnections.add(inSocket);
			inSocket.lastCommunicatedTime = currentTime;
		}
	}
	private void readFromConnections() throws IOException {
		int read = readSelector.selectNow();
		if(read > 0){
			Set<SelectionKey> selectedKeys = readSelector.selectedKeys();
			Iterator<SelectionKey> itr = selectedKeys.iterator();
			while(itr.hasNext()){
				SelectionKey key = itr.next();
				readConnection(key);
				itr.remove();
			}
			selectedKeys.clear();
		}
	}
	
	private void readConnection(SelectionKey key) throws IOException {
		TCPConnection c = (TCPConnection) key.attachment();
		c.lastCommunicatedTime = currentTime;
		try {
			c.reader.read(c);
		} catch(IOException e){
			closeConnection(c);
			throw e;
		}
		if(c.isClosed){
			c.channel.close();
			activeConnections.remove(c);
			System.out.println("Client disconnected: " + c);
			if(log != null){
				log.addLast("Client disconnected: ");
				log.addLast(c.toString());
			}
		}
	}
	
	private void writeToConnections() throws IOException {
		int write = writeSelector.selectNow();
		if(write > 0){
			Set<SelectionKey> selectedKeys = writeSelector.selectedKeys();
			Iterator<SelectionKey> itr = selectedKeys.iterator();
			while(itr.hasNext()){
				SelectionKey key = itr.next();
				writeConnection(key);
				itr.remove();
			}
			selectedKeys.clear();
		}
	}
	
	private void writeConnection(SelectionKey key) throws IOException {
		TCPConnection c = (TCPConnection) key.attachment();
		try {
			c.writer.write(c);
		} catch(IOException e){
			closeConnection(c);
			throw e;
		}
	}
	
	private void init(){
	}
}
