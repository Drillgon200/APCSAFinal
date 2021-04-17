package com.drillgon200.shooter.entity;

import com.drillgon200.shooter.World;

public class PlayerClient extends Player {

	public PlayerClient(World w) {
		super(w);
	}
	
	@Override
	public void update() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		super.update();
	}
	
	public PlayerClient(World w, float posX, float posY, float posZ) {
		super(w, posX, posY, posZ);
	}

}
