package com.drillgon200.shooter.entity;

import com.drillgon200.shooter.MainConfig;
import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.Vec3f;

public class Entity {

	public World world;
	
	public double posX;
	public double posY;
	public double posZ;
	public double prevPosX;
	public double prevPosY;
	public double prevPosZ;
	public double motionX;
	public double motionY;
	public double motionZ;
	
	public boolean hasGravity = false;
	
	public double rotationPitch = 0;
	public double rotationYaw = 0;
	
	public Entity(World w) {
		this(w, 0, 0, 0);
	}
	
	public Entity(World w, double posX, double posY, double posZ) {
		this.world = w;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
	}
	
	public void update(){
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if(hasGravity){
			motionY -= MainConfig.GRAVITY*MainConfig.TICKRATE_RCP;
		}
		move(motionX, motionY, motionZ);
		
		motionX = 0;
		motionY = 0;
		motionZ = 0;
	}
	
	public void move(double mX, double mY, double mZ){
		posX += mX;
		posY += mY;
		posZ += mZ;
	}
	
	public float getEyeHeight(){
		return 2;
	}
	
	public Vec3f getInterpolatedPos(float partialTicks){
		if(partialTicks == 1){
			return new Vec3f((float)posX, (float)posY, (float)posZ);
		}
		float x = (float) (prevPosX + (posX-prevPosX)*partialTicks);
		float y = (float) (prevPosY + (posY-prevPosY)*partialTicks);
		float z = (float) (prevPosZ + (posZ-prevPosZ)*partialTicks);
		return new Vec3f(x, y, z);
	}
	
	public void addVelocity(Vec3f motion){
		motionX += motion.x;
		motionY += motion.y;
		motionZ += motion.z;
	}
}
