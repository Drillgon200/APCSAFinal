package com.drillgon200.shooter.entity;

import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.Vec3f;

public class Player extends Entity {

	public int input_moveForward;
	public int input_moveStrafe;
	public Vec3f lookVec;
	public Vec3f rightVec;
	public Vec3f leftVec;
	
	public Player(World w, double posX, double posY, double posZ) {
		super(w, posX, posY, posZ);
	}

}
