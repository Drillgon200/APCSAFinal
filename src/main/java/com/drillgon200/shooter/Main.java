package com.drillgon200.shooter;

public class Main {

	public static void main(String[] args) {
		System.out.println("Starting game...");
		Shooter.init();
		Shooter.renderLoop();
		System.out.println("Exited game.");
	}

}