package com.drillgon200.physics;

import com.drillgon200.physics.GJK.GJKInfo;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Vec3f;

public class Contact {

	public RigidBody bodyA;
	public RigidBody bodyB;
	public Collider a;
	public Collider b;
	public Vec3f localA;
	public Vec3f localB;
	public Vec3f globalA;
	public Vec3f globalB;
	public Vec3f normal;
	public float depth;
	public Vec3f tangent;
	public Vec3f bitangent;
	
	public Vec3f rA;
	public Vec3f rB;
	
	public Jacobian normalContact;
	public Jacobian tangentContact;
	public Jacobian bitangentContact;
	
	public Contact(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, GJKInfo info) {
		this.a = a;
		this.b = b;
		if(bodyA == null){
			bodyA = RigidBody.DUMMY;
		}
		if(bodyB == null){
			bodyB = RigidBody.DUMMY;
		}
		this.bodyA = bodyA;
		this.bodyB = bodyB;
		localA = bodyA.globalToLocalPos(info.contactPointA);
		localB = bodyB.globalToLocalPos(info.contactPointB);
		globalA = info.contactPointA;
		globalB = info.contactPointB;
		normal = info.normal;
		depth = info.depth;
		//https://box2d.org/posts/2014/02/computing-a-basis/
		if(Math.abs(normal.x) >= 0.57735){
			tangent = new Vec3f(normal.y, -normal.x, 0).normalize();
		} else {
			tangent = new Vec3f(0, normal.z, -normal.y).normalize();
		}
		bitangent = normal.cross(tangent);
		
		normalContact = new Jacobian(false);
		tangentContact = new Jacobian(true);
		bitangentContact = new Jacobian(true);
	}
	
	public void init(float dt){
		rA = globalA.subtract(bodyA == RigidBody.DUMMY ? a.localCentroid : bodyA.globalCentroid);
		rB = globalB.subtract(bodyB == RigidBody.DUMMY ? b.localCentroid : bodyB.globalCentroid);
		
		normalContact.init(this, normal, dt);
		tangentContact.init(this, tangent, dt);
		bitangentContact.init(this, bitangent, dt);
	}
	
	public void solve(float dt){
		normalContact.solve(this, dt);
		tangentContact.solve(this, dt);
		bitangentContact.solve(this, dt);
	}
	
	public static class Jacobian {
		
		boolean tangent;
		
		Vec3f j_va;
		Vec3f j_wa;
		Vec3f j_vb;
		Vec3f j_wb;
		
		float bias;
		double effectiveMass;
		double totalLambda;
		
		public Jacobian(boolean tangent) {
			this.tangent = tangent;
		}
		
		public void init(Contact c, Vec3f dir, float dt){
			j_va = dir.negate();
			j_wa = c.rA.cross(dir).negate();
			j_vb = dir;
			j_wb = c.rB.cross(dir);
			
			bias = 0.0F;
			if(!tangent){
				float beta = 0.2F;
				bias = -(beta/dt)*c.depth;
			}
			
			effectiveMass = 
					  c.bodyA.inv_mass
					+ j_wa.dot(j_wa.matTransform(c.bodyA.inv_globalInertiaTensor))
					+ c.bodyB.inv_mass
					+ j_wb.dot(j_wb.matTransform(c.bodyB.inv_globalInertiaTensor));
			effectiveMass = 1D/effectiveMass;
			
			totalLambda = 0;
		}
		
		public void solve(Contact c, float dt){
			double jv = 
					  j_va.dot(c.bodyA.linearVelocity)
					+ j_wa.dot(c.bodyA.angularVelocity)
					+ j_vb.dot(c.bodyB.linearVelocity)
					+ j_wb.dot(c.bodyB.angularVelocity);
			double lambda = effectiveMass * (-(jv + bias));
			double oldTotalLambda = totalLambda;
			if(tangent){
				float friction = c.bodyA.friction*c.bodyB.friction;
				double maxFriction = friction*c.normalContact.totalLambda;
				totalLambda = MathHelper.clamp(totalLambda + lambda, -maxFriction, maxFriction);
			} else {
				totalLambda = Math.max(0, totalLambda + lambda);
			}
			lambda = totalLambda - oldTotalLambda;	
			
			c.bodyA.addLinearVelocity(j_va.scaled(c.bodyA.inv_mass * lambda));
			c.bodyA.addAngularVelocity(j_wa.matTransform(c.bodyA.inv_globalInertiaTensor).scaled(lambda));
			c.bodyB.addLinearVelocity(j_vb.scaled(c.bodyB.inv_mass * lambda));
			c.bodyB.addAngularVelocity(j_wb.matTransform(c.bodyB.inv_globalInertiaTensor).scaled(lambda));
		}
	}
}
