package com.drillgon200.shooter.entity;

import com.drillgon200.networking.Connection;
import com.drillgon200.physics.CapsuleCollider;
import com.drillgon200.physics.Collider;
import com.drillgon200.physics.RigidBody;
import com.drillgon200.physics.TriangleCollider;
import com.drillgon200.shooter.Keybindings;
import com.drillgon200.shooter.MainConfig;
import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Vec3f;

public abstract class Player extends Entity {

	public Connection connection;
	
	public int input_moveForward;
	public int input_moveStrafe;
	public Vec3f lookVec;
	public Vec3f forwardVec;
	public Vec3f backVec;
	public Vec3f rightVec;
	public Vec3f leftVec;
	
	public Player(World w) {
		this(w, 0, 0, 0);
	}
	
	public Player(World w, float posX, float posY, float posZ) {
		super(w, posX, posY, posZ);
		lookVec = new Vec3f((float)Math.toRadians(-rotationYaw-90), (float)Math.toRadians(rotationPitch+180));
		rightVec = new Vec3f((float)Math.toRadians(-rotationYaw), 0);
		leftVec = rightVec.negate();
		forwardVec = new Vec3f((float)Math.toRadians(-rotationYaw+90), 0);
		backVec = forwardVec.negate();
	}

	@Override
	protected RigidBody initRigidBody() {
		RigidBody body = new RigidBody(world, posX, posY, posZ);
		float height = 2.2F;
		float radius = 0.4F;
		height -= radius*2;
		Collider c = new CapsuleCollider(new Vec3f(0, 0, 0), height, radius, 1);
		body.addColliders(c);
		//body.addColliders(new AABBCollider(new AxisAlignedBB(-0.5F, 0F, -0.5F, 0.5F, 2F, 0.5F), 1));
		body.friction = 0F;
		body.gravity *= 4F;
		body.linearDrag = 0.7F;
		body.restitution = 0;
		body.lockRotation(true, true, true);
		return body;
	}
	
	@Override
	public void update() {
		age ++;
		if(world.isRemote()){
			//Players need to only do this stuff on client so it feels good to play.
			//I'm not going to worry about cheating right now.
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
		if(this.posY <= -100){
			Vec3f spawn = world.getLevel().possibleSpawns.get(world.rand.nextInt(world.getLevel().possibleSpawns.size()));
			this.setPos(spawn);
			this.body.linearVelocity = new Vec3f(0, 0, 0);
		}
	}
	
	@Override
	public float getEyeHeight() {
		if(Keybindings.sneak.isDown){
			return super.getEyeHeight()-0.2F;
		}
		return super.getEyeHeight();
	}
	
}
