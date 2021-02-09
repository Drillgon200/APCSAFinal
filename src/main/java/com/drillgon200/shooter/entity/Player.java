package com.drillgon200.shooter.entity;

import com.drillgon200.physics.CapsuleCollider;
import com.drillgon200.physics.Collider;
import com.drillgon200.physics.RigidBody;
import com.drillgon200.shooter.Keybindings;
import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.Vec3f;

public class Player extends Entity {

	public int input_moveForward;
	public int input_moveStrafe;
	public Vec3f lookVec;
	public Vec3f forwardVec;
	public Vec3f backVec;
	public Vec3f rightVec;
	public Vec3f leftVec;
	
	public Player(World w, float posX, float posY, float posZ) {
		super(w, posX, posY, posZ);
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
		body.gravity *= 2.4F;
		body.linearDrag = 0.7F;
		body.lockRotation(true, true, true);
		return body;
	}
	
	@Override
	public void update() {
		super.update();
		if(this.posY <= -10){
			this.setPos(0, 4, 0);
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
