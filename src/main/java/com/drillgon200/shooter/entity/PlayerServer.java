package com.drillgon200.shooter.entity;

import com.drillgon200.shooter.World;

public class PlayerServer extends Player {

	public PlayerServer(World w) {
		super(w);
	}
	
	public PlayerServer(World w, float posX, float posY, float posZ) {
		super(w, posX, posY, posZ);
	}
	
	@Override
	public void update() {
		super.update();
	}

}
