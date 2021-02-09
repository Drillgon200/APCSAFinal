package com.drillgon200.physics;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.drillgon200.physics.GJK.GJKInfo;
import com.drillgon200.physics.GJK.Result;
import com.drillgon200.shooter.MainConfig;
import com.drillgon200.shooter.World;
import com.drillgon200.shooter.util.AxisAngle4f;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Quat4f;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.Vec3f;
import com.drillgon200.shooter.util.VertexFormat;

public class RigidBody {
	
	public static final FloatBuffer AUX_GL_BUFFER = GLAllocation.createDirectFloatBuffer(16);

	public static final Vec3f[] cardinals = new Vec3f[]{
			new Vec3f(1, 0, 0), new Vec3f(0, 1, 0), new Vec3f(0, 0, 1),
			new Vec3f(-1, 0, 0), new Vec3f(0, -1, 0), new Vec3f(0, 0, -1)};
	
	public static final RigidBody DUMMY = new RigidBody(null){
		public void solveContacts(float dt) {};
		
		public void impulse(Vec3f force, Vec3f position) {};
		public void updateOrientation() {};
		public void updateGlobalCentroidFromPosition() {};
		public void updatePositionFromGlobalCentroid() {};
		public void doTimeStep(float dt) {};
		public void addColliders(Collider... collide) {};
		
		public Vec3f globalToLocalPos(Vec3f pos) {return pos;};
		public Vec3f localToGlobalPos(Vec3f pos) {return pos;};
		public Vec3f globalToLocalVec(Vec3f vec) {return vec;};
		public Vec3f localToGlobalVec(Vec3f vec) {return vec;};
		
		public void addLinearVelocity(Vec3f v) {};
		public void addAngularVelocity(Vec3f v) {};
		
		public void addContact(Contact c) {};
	};
	
	static {
		DUMMY.inv_rotation = (Matrix3f) DUMMY.rotation.clone();
		DUMMY.localInertiaTensor = new Matrix3f();
		DUMMY.inv_localInertiaTensor = new Matrix3f().setZero();
		DUMMY.inv_globalInertiaTensor = new Matrix3f().setZero();
		DUMMY.inv_mass = 0;
		DUMMY.localCentroid = new Vec3f(0, 0, 0);
		DUMMY.globalCentroid = new Vec3f(0, 0, 0);
		DUMMY.friction = 1;
	}
	
	public World world;
	public AxisAlignedBB boundingBox;
	
	public List<Collider> colliders = new ArrayList<>();
	public List<AxisAlignedBB> colliderBoundingBoxes = new ArrayList<>();
	
	public Vec3f position = new Vec3f(0, 0, 0);
	public Vec3f globalCentroid;
	public Matrix3f rotation = new Matrix3f();
	public Matrix3f inv_rotation;
	
	public Vec3f prevPosition = new Vec3f(0, 0, 0);
	public Quat4f prevRotation = new Quat4f();
	
	public Vec3f linearVelocity = new Vec3f(0, 0, 0);
	public Vec3f angularVelocity = new Vec3f(0, 0, 0);
	public Vec3f force = new Vec3f(0, 0, 0);
	public Vec3f torque = new Vec3f(0, 0, 0);
	
	public float mass;
	public float inv_mass;
	public Matrix3f localInertiaTensor;
	public Matrix3f inv_localInertiaTensor;
	public Matrix3f inv_globalInertiaTensor;
	public float friction = 0.95F;
	public float linearDrag = 0.2F;
	public float angularDrag = 0.2F;
	public float gravity = -9.81F;
	
	public boolean xLocked, yLocked, zLocked;
	
	public Vec3f localCentroid;
	
	public ContactManifold contacts = new ContactManifold();
	
	public RigidBody(World w) {
		this.world = w;
		rotation.setIdentity();
	}
	
	public RigidBody(World w, float x, float y, float z) {
		this(w);
		this.position = new Vec3f(x, y, z);
	}
	
	public void lockRotation(boolean x, boolean y, boolean z){
		xLocked = x;
		yLocked = y;
		zLocked = z;
		updateOrientation();
	}
	
	public void subdivTimestep(){
		this.setPrevData();
		int timeStepSubDiv = 2;
		float step = MainConfig.TICKRATE_RCP/(float)timeStepSubDiv;
		for(int i = 0; i < timeStepSubDiv; i ++){
			doTimeStep(step);
		}
	}
	
	public void doTimeStep(float dt){
		contacts.update();
		//Do collision detection
		GJKInfo bestInfo = null;
		Collider a = null;
		Collider b = null;
		List<Collider> l = world.static_colliders.getObjectsIntersectingAABB(boundingBox);
		for(Collider wCollider : l){
			for(int i = 0; i < colliders.size(); i ++){
				Collider c = colliders.get(i);
				if(!colliderBoundingBoxes.get(i).intersects(wCollider.getBoundingBox()))
					continue;
				b = wCollider;
				GJKInfo info = GJK.colliding(this, null, c, b);
				if(info.result == Result.COLLIDING && (bestInfo == null || bestInfo.depth < info.depth)){
					a = c;
					bestInfo = info;
				}
			}
		}
		
		if(bestInfo != null){
			contacts.addContact(new Contact(this, null, a, b, bestInfo));
		}
		
		solveContacts(dt);
		integrateVelocityAndPosition(dt);
	}
	
	public void integrateVelocityAndPosition(float dt){
		//Integrate velocity
		linearVelocity = linearVelocity.add(force.scale(inv_mass*dt));
		angularVelocity = angularVelocity.add(torque.scale(dt).matTransform(inv_globalInertiaTensor));
		if(xLocked && yLocked && zLocked){
			angularVelocity.set(0, 0, 0);
		}
				
		force.set(0, 0, 0);
		torque.set(0, 0, 0);
				
		//Integrate position
		globalCentroid = globalCentroid.add(linearVelocity.scale(dt));
		if(angularVelocity.lenSq() > 0){
			Vec3f axis = angularVelocity.normalize();
			float angle = angularVelocity.len()*dt;
			Matrix3f turn = new Matrix3f();
			turn.set(new AxisAngle4f(axis.x, axis.y, axis.z, angle));
			turn.mul(rotation);
			rotation = turn;
			updateOrientation();
		}
		updatePositionFromGlobalCentroid();
		updateAABBs();
		this.linearVelocity = linearVelocity.scaled(Math.pow(1-linearDrag, dt));
		this.angularVelocity = angularVelocity.scaled(Math.pow(1-angularDrag, dt));
		addLinearVelocity(new Vec3f(0, gravity*dt, 0));
	}
	
	public void setPrevData(){
		this.prevPosition.set(position);
		prevRotation.setFromMat(rotation);
	}
	
	public void addContact(Contact c){
		contacts.addContact(c);
	}
	
	public void solveContacts(float dt){
		for(int j = 0; j < contacts.contactCount; j ++){
			contacts.contacts[j].init(dt);
		}
		int velocityIterations = 8;
		for(int i = 0; i < velocityIterations; i ++){
			for(int j = 0; j < contacts.contactCount; j ++){
				contacts.contacts[j].solve(dt);
			}
		}
	}
	
	/*public void solveContacts(float dt){
		for(int i = 0; i < contacts.contactCount; i ++){
			Contact c = contacts.contacts[i];
			RigidBody bodyA = c.bodyA;
			RigidBody bodyB = c.bodyB;
			Vec3f contactNormal = c.normal;
			if(bodyA == RigidBody.DUMMY || bodyA != this){
				bodyA = c.bodyB;
				bodyB = c.bodyA;
				contactNormal = contactNormal.negate();
			}
			Vec3f rA = c.localA.subtract(c.a.localCentroid);
			Vec3f rB = c.localB.subtract(c.b.localCentroid);
			Vec3f[] jacobian = new Vec3f[]{contactNormal.negate(), rA.crossProduct(contactNormal).negate(), contactNormal, rB.crossProduct(contactNormal)};
			double inv_effectiveMass = 
				  bodyA.inv_mass
				+ jacobian[1].dotProduct(jacobian[1].matTransform(bodyA.inv_globalInertiaTensor))
				+ bodyB.inv_mass
				+ jacobian[3].dotProduct(jacobian[3].matTransform(bodyB.inv_globalInertiaTensor));
			
			double jv = 
				  jacobian[0].dotProduct(bodyA.linearVelocity)
				+ jacobian[1].dotProduct(bodyA.angularVelocity)
				+ jacobian[2].dotProduct(bodyB.linearVelocity)
				+ jacobian[3].dotProduct(bodyB.angularVelocity);
			
			float beta = 0.2F;
			float b = -(beta/dt)*c.depth;
			
			float lambda = (float) ((-(jv+b))/inv_effectiveMass);
			//float oldTotalLambda = c.totalLambda;
			//c.totalLambda = Math.max(0, c.totalLambda + lambda);
			//lambda = c.totalLambda - oldTotalLambda;
			
			bodyA.addLinearVelocity(jacobian[0].scale(bodyA.inv_mass*lambda));
			bodyA.addAngularVelocity(jacobian[1].matTransform(bodyA.inv_globalInertiaTensor).scale(lambda));
			bodyB.addLinearVelocity(jacobian[2].scale(bodyB.inv_mass*lambda));
			bodyB.addAngularVelocity(jacobian[3].matTransform(bodyB.inv_globalInertiaTensor).scale(lambda));
		}
		
	}*/
	
	public Vec3f localToGlobalPos(Vec3f pos){
		return pos.matTransform(rotation).add(position);
	}
	public Vec3f globalToLocalPos(Vec3f pos){
		return pos.subtract(position).matTransform(inv_rotation);
	}
	public Vec3f localToGlobalVec(Vec3f vec){
		return vec.matTransform(rotation);
	}
	public Vec3f globalToLocalVec(Vec3f vec){
		return vec.matTransform(inv_rotation);
	}
	
	public void addLinearVelocity(Vec3f v){
		linearVelocity = linearVelocity.add(v);
	}
	public void addAngularVelocity(Vec3f v){
		angularVelocity = angularVelocity.add(v);
	}
	public void impulse(Vec3f force, Vec3f position){
		this.force = this.force.add(force);
		this.torque = this.torque.add(position.subtract(globalCentroid).cross(force));
	}
	
	public void impulseVelocity(Vec3f force, Vec3f position){
		linearVelocity = linearVelocity.add(force.scale(inv_mass));
		angularVelocity = angularVelocity.add(position.subtract(globalCentroid).cross(force).matTransform(inv_globalInertiaTensor));
	}
	
	public void setPosition(Vec3f vec) {
		this.position = vec;
		this.updateGlobalCentroidFromPosition();
	}
	
	public void updateOrientation(){
		Quat4f quat = new Quat4f();
		//quat.set(rotation);
		quat.setFromMat(rotation);
		quat.normalize();
		quat.matrixFromQuat(rotation);
		//System.out.println("1");
		inv_rotation = (Matrix3f) rotation.clone();
		inv_rotation.transpose();
		
		/*Matrix3f bruh = new Matrix3f(0.44135904F, -0.0038072586F, 0.89732254F,
				-0.004132689F, -0.9999893F, -0.0022101535F,
				0.8973211F, -0.002732877F, -0.44137013F);
		
		System.out.println(bruh.m00 + bruh.m11 + bruh.m22 + 1.0f);
		quat.set(bruh);
		System.out.println(quat);
		quat.normalize();
		System.out.println(quat);
		BobMathUtil.matrixFromQuat(bruh, quat);
		System.out.println(bruh);*/
		
		inv_globalInertiaTensor.set(rotation);
		inv_globalInertiaTensor.mul(inv_localInertiaTensor);
		inv_globalInertiaTensor.mul(inv_rotation);
		if(xLocked){
			inv_globalInertiaTensor.m00 = 0;
			inv_globalInertiaTensor.m01 = 0;
			inv_globalInertiaTensor.m02 = 0;
		}
		if(yLocked){
			inv_globalInertiaTensor.m10 = 0;
			inv_globalInertiaTensor.m11 = 0;
			inv_globalInertiaTensor.m12 = 0;
		}
		if(zLocked){
			inv_globalInertiaTensor.m20 = 0;
			inv_globalInertiaTensor.m21 = 0;
			inv_globalInertiaTensor.m22 = 0;
		}
	}
	public void updatePositionFromGlobalCentroid(){
		position = globalCentroid.add(localCentroid.scale(-1).matTransform(rotation));
	}
	public void updateGlobalCentroidFromPosition(){
		globalCentroid = localCentroid.matTransform(rotation).add(position);
	}
	public void updateAABBs(){
		colliderBoundingBoxes.clear();
		float tMaxX, tMaxY, tMaxZ, tMinX, tMinY, tMinZ;
		tMaxX = tMaxY = tMaxZ = -Float.MAX_VALUE;
		tMinX = tMinY = tMinZ = Float.MAX_VALUE;
		for(Collider c : colliders){
			float maxX = GJK.localSupport(this, c, cardinals[0]).x;
			float maxY = GJK.localSupport(this, c, cardinals[1]).y;
			float maxZ = GJK.localSupport(this, c, cardinals[2]).z;
			float minX = GJK.localSupport(this, c, cardinals[3]).x;
			float minY = GJK.localSupport(this, c, cardinals[4]).y;
			float minZ = GJK.localSupport(this, c, cardinals[5]).z;
			colliderBoundingBoxes.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
			tMaxX = Math.max(tMaxX, maxX);
			tMaxY = Math.max(tMaxY, maxY);
			tMaxZ = Math.max(tMaxZ, maxZ);
			tMinX = Math.min(tMinX, minX);
			tMinY = Math.min(tMinY, minY);
			tMinZ = Math.min(tMinZ, minZ);
		}
		boundingBox = new AxisAlignedBB(tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
	}
	
	public void addColliders(Collider... collide){
		for(Collider c : collide){
			colliders.add(c);
		}
		localCentroid = new Vec3f(0, 0, 0);
		mass = 0;
		for(Collider c : colliders){
			mass += c.mass;
			localCentroid = localCentroid.add(c.localCentroid.scale(c.mass));
		}
		inv_mass = 1F/mass;
		localCentroid = localCentroid.scale(inv_mass);
		
		localInertiaTensor = new Matrix3f().setZero();
		for(Collider c : colliders){
			//https://en.wikipedia.org/wiki/Parallel_axis_theorem
			Vec3f colliderToLocal = localCentroid.subtract(c.localCentroid);
			double lenSquared = colliderToLocal.dot(colliderToLocal);
			Matrix3f outerProduct = colliderToLocal.outerProduct(colliderToLocal);
			
			Matrix3f colliderToLocalMat = new Matrix3f();
			colliderToLocalMat.setIdentity();
			colliderToLocalMat.mul((float) lenSquared);
			colliderToLocalMat.sub(outerProduct);
			colliderToLocalMat.mul(c.mass);
			Matrix3f cLocalIT = (Matrix3f) c.localInertiaTensor.clone();
			cLocalIT.add(colliderToLocalMat);
			localInertiaTensor.add(cLocalIT);
		}
		inv_localInertiaTensor = new Matrix3f();
		inv_localInertiaTensor.set(localInertiaTensor);
		
		inv_localInertiaTensor.invert();
		inv_globalInertiaTensor = new Matrix3f();
		updateOrientation();
		updateGlobalCentroidFromPosition();
		this.prevPosition = position;
		updateAABBs();
	}
	
	public void doGlTransform(Vec3f playerPos, float partialTicks){
		FloatBuffer buf = AUX_GL_BUFFER;
		Quat4f quat = new Quat4f();
		quat.setFromMat(rotation);
		quat.interpolate(prevRotation, 1-partialTicks);
		quat.normalize();
		Matrix3f rotation = quat.matrixFromQuat();
		
		buf.put(0, rotation.m00);
		buf.put(1, rotation.m10);
		buf.put(2, rotation.m20);
		buf.put(3, 0);
		buf.put(4, rotation.m01);
		buf.put(5, rotation.m11);
		buf.put(6, rotation.m21);
		buf.put(7, 0);
		buf.put(8, rotation.m02);
		buf.put(9, rotation.m12);
		buf.put(10, rotation.m22);
		buf.put(11, 0);
		
		Vec3f pos = this.prevPosition.add(this.position.subtract(this.prevPosition).scale(partialTicks)).subtract(playerPos);
		
		buf.put(12, (float)pos.x);
		buf.put(13, (float)pos.y);
		buf.put(14, (float)pos.z);
		buf.put(15, 1);
		
		GL11.glMultMatrixf(buf);
		
		buf.rewind();
	}
	
	public void renderDebugInfo(Vec3f offset, float partialTicks){
		GL11.glPushMatrix();
		Tessellator tes = Tessellator.instance;
		for(Contact c : contacts.contacts){
			if(c != null){
				tes.begin(GL11.GL_LINES, VertexFormat.POSITION);
				Vec3f normal = c.normal.scale(0.5F);
				Vec3f globalA = c.globalA;
				Vec3f globalB = c.globalB;
				tes.pos(globalA.x, globalA.y, globalA.z).endVertex();
				tes.pos(globalA.x-normal.x, globalA.y-normal.y, globalA.z-normal.z).endVertex();
				
				tes.pos(globalB.x, globalB.y, globalB.z).endVertex();
				tes.pos(globalB.x+normal.x, globalB.y+normal.y, globalB.z+normal.z).endVertex();
				
				tes.pos(position.x, position.y, position.z).endVertex();
				tes.pos(position.x+angularVelocity.x, position.y+angularVelocity.y, position.z+angularVelocity.z).endVertex();
				tes.draw();
				
				GL11.glPointSize(16);
				tes.begin(GL11.GL_POINTS, VertexFormat.POSITION);
				tes.pos(globalA.x, globalA.y, globalA.z).endVertex();
				tes.pos(globalB.x, globalB.y, globalB.z).endVertex();
				tes.pos(position.x, position.y, position.z).endVertex();
				tes.draw();
			}
		}
		doGlTransform(offset, partialTicks);
		for(Collider c : colliders){
			c.debugRender();
		}
		GL11.glPopMatrix();
	}
}
