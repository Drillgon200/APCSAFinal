package com.drillgon200.shooter.gui;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.drillgon200.shooter.PacketDispatcher;
import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.packets.CPacketJoinServer;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.TextureManager;

public class TitleScreen extends GuiScreen {

	//Only displayed in this gui so it makes sense to put it here.
	public static Deque<String> serverLog = new ConcurrentLinkedDeque<>();
	public static Deque<String> clientLog = new ConcurrentLinkedDeque<>();
	
	public TitleScreen() {
		float yOffset = 50;
		float xOffset = 33;
		float scale = 60;
		float width = 300;
		GuiElementMenuButton main = new GuiElementMenuButton(this);
		this.elements.add(main);
		
		
		GuiElementMenuButton play = new GuiElementMenuButton(this, main, 20, 600, width, scale, "Play");
		play.children.add(new GuiElementInfoBox(this, 20, 450, 500, 500, clientLog));
		GuiElementTextBox ip = new GuiElementTextBox(this, 20, 350, 200, 120, "Server IP (default: localhost)", 20);
		GuiElementTextBox joinPort = new GuiElementTextBox(this, 20, 275, 200, 120, "Server port (default: 46655)", 5);
		play.children.add(ip);
		play.children.add(joinPort);
		play.children.add(new GuiElementButton(this, 20, 200, 2.5F, 80, "Join", true, () -> {
			if(Shooter.state != ConnectionState.DISCONNECTED){
				clientLog.addLast("Already connecting or connected!");
				return;
			}
			int port = getPortFromString(clientLog, joinPort.text, 46655);
			if(port == -1)
				return;
			String ipName = ip.text;
			
			clientLog.addLast("Attempting connection on " + (ipName.isEmpty() ? "localhost" : ipName) + ":" + port + "...");
			Shooter.connection.setLog(clientLog);
			new Thread(() -> {
				Shooter.state = ConnectionState.CONNECTING;
				try {
					Shooter.connection.connect(InetAddress.getByName(ipName.isEmpty() ? "localhost" : ipName), port);
					clientLog.addLast("Connection accepted, attempting join...");
					System.out.println("Sending join packet...");
					PacketDispatcher.sendToServer(new CPacketJoinServer());
					return;
				} catch(UnknownHostException e) {
					clientLog.addLast("Unknown host, connect failed.");
				} catch(ConnectException e){
					clientLog.addLast("Connection refused.");
				} catch(IOException e){
					e.printStackTrace();
					clientLog.addLast("Unknown IOException.");
				}
				Shooter.state = ConnectionState.DISCONNECTED;
			}).start();
		}));
		//play.children.add(new GuiElementMenuButton(this, main, 20+xOffset, 600-yOffset, width, scale, "Tester"));
		//play.children.add(new GuiElementMenuButton(this, main, 20+xOffset*2, 600-yOffset*2, width, scale, "Fat Duck"));
		
		GuiElementMenuButton server = new GuiElementMenuButton(this, main, 20+xOffset, 600-yOffset, width, scale, "Start Server");
		server.children.add(new GuiElementInfoBox(this, 20, 450, 500, 500, serverLog));
		GuiElementTextBox portBox = new GuiElementTextBox(this, 20, 350, 50, 120, "Server port (default: 46655)", 5);
		server.children.add(portBox);
		server.children.add(new GuiElementTextBox(this, 20, 275, 200, 120, "Name", 20));
		server.children.add(new GuiElementButton(this, 20, 200, 2.5F, 80, "Start server", true, ()->{
			int port = getPortFromString(serverLog, portBox.text, 46655);
			if(port == -1)
				return;
			ShooterServer.start_async(serverLog, port);
		}));
		server.children.add(new GuiElementButton(this, 20, 125, 2.5F, 80, "Stop server", true, ()->{
			ShooterServer.shutdown_async(serverLog);
		}));
		
		
		main.children.add(play);
		main.children.add(server);
		main.children.add(new GuiElementMenuButton(this, main, 20+xOffset*2, 600-yOffset*2, width, scale, "Options"));
		main.setParents();
	}
	
	public int getPortFromString(Deque<String> log, String text, int def){
		int port;
		if(text.isEmpty()){
			port = def;
		} else {
			try {
				port = Integer.parseInt(text);
			} catch(NumberFormatException e){
				log.addLast("Port not a number!");
				return -1;
			}
			//According to google, 1024 and below are reserved and 65535 is the default max on windows
			if(port < 1024 || port > 65535){
				log.addLast("Port out of range! Must be between 1024 and 65535.");
				return -1;
			}
		}
		return port;
	}
	
	@Override
	public void drawGuiBackgroundLayer(float w, float h) {
		TextureManager.bindTexture(Resources.ctf_arena);
		RenderHelper.drawGuiRect(0, 0, w, h, 0, 0, 1, 1);
	}
	
	@Override
	public boolean close() {
		return false;
	}
}
