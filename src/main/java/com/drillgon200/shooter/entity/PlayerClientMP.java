package com.drillgon200.shooter.entity;

import com.drillgon200.physics.RigidBody;
import com.drillgon200.shooter.World;

public class PlayerClientMP extends Player {

	public PlayerClientMP(World w) {
		super(w);
	}
	
	@Override
	protected RigidBody initRigidBody() {
		return super.initRigidBody();
	}
	
	@Override
	public void update() {
		age ++;
	}
	
	public PlayerClientMP(World w, float posX, float posY, float posZ) {
		super(w, posX, posY, posZ);
	}

}
