package com.drillgon200.shooter.entity;

import java.util.List;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.Collider;
import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.physics.RigidBody;
import com.drillgon200.shooter.MainConfig;
import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.Vec3f;

public class Entity {

	public short entityId;
	
	public World world;
	
	public RigidBody body;
	
	protected float posX;
	protected float posY;
	protected float posZ;
	protected float prevPosX;
	protected float prevPosY;
	protected float prevPosZ;
	public float motionX;
	public float motionY;
	public float motionZ;
	public boolean isOnGround;
	public int airTicks = 0;
	
	public boolean markedForRemoval = false;
	
	public int age;
	
	public boolean hasGravity = false;
	
	public double rotationPitch = 0;
	public double rotationYaw = 0;
	
	public Entity(World w) {
		this(w, 0, 0, 0);
	}
	
	public Entity(World w, float posX, float posY, float posZ) {
		this.world = w;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		this.body = initRigidBody();
	}
	
	public void update(){
		age ++;
		if(!world.isRemote()){
			if(hasGravity){
				motionY -= MainConfig.GRAVITY*MainConfig.TICKRATE_RCP;
			}
			if(body != null){
				body.addLinearVelocity(new Vec3f(motionX, motionY, motionZ));
				body.subdivTimestep();
				this.posX = body.position.x;
				this.posY = body.position.y;
				this.posZ = body.position.z;
			} else {
				move(motionX, motionY, motionZ);
			}
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
			updateGroundState();
		}
	}
	
	public void updatePositionFromServer(Vec3f pos){
		if(body != null){
			body.prevPosition = body.position;
			body.setPosition(pos);
		} else {
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			posX = pos.x;
			posY = pos.y;
			posZ = pos.z;
		}
	}
	
	public void move(double mX, double mY, double mZ){
		List<Collider> list = world.getLevel().staticColliders.getObjectsIntersectingAABB(new AxisAlignedBB(-0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F).offset((float)posX, (float)posY, (float)posZ));
		if(!list.isEmpty() && mY < 0)
			return;
		posX += mX;
		posY += mY;
		posZ += mZ;
	}
	
	public void updateGroundState(){
		if(body != null){
			isOnGround = false;
			RayTraceResult res = world.rayCast(new Vec3f(posX, posY+0.1F, posZ), new Vec3f(posX, posY-0.1F, posZ));
			if(res.hit)
				this.isOnGround = true;
			else
				this.isOnGround = false;
		} else {
			RayTraceResult res = world.rayCast(new Vec3f(posX, posY, posZ), new Vec3f(posX, posY-0.02F, posZ));
			if(res.hit)
				this.isOnGround = true;
			else
				this.isOnGround = false;
		}
		if(isOnGround){
			airTicks = 0;
		} else {
			airTicks ++;
		}
	}
	
	public void setPos(Vec3f vec){
		setPos(vec.x, vec.y, vec.z);
	}
	
	public void setPos(float x, float y, float z){
		if(body == null){
			prevPosX = x;
			prevPosY = y;
			prevPosZ = z;
			this.posX = x;
			this.posY = y;
			this.posZ = z;
		} else {
			body.setPosition(new Vec3f(x, y, z));
			body.prevPosition = body.position;
		}
	}
	
	public Vec3f getVelocity(){
		if(body == null){
			return new Vec3f(motionX, motionY, motionZ);
		} else {
			return body.linearVelocity;
		}
	}
	
	protected RigidBody initRigidBody(){
		return null;
	}
	
	public float getEyeHeight(){
		return 1.8F;
	}
	
	public Vec3f getInterpolatedPos(float partialTicks){
		if(body != null){
			if(partialTicks == 1)
				return body.position;
			else
				return body.prevPosition.add(body.position.subtract(body.prevPosition).scale(partialTicks));
		}
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
