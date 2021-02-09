package com.drillgon200.physics;

import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.Vec3f;

public class CapsuleCollider extends Collider {

	public Vec3f pos;
	public float height, radius;
	public AxisAlignedBB box;
	
	private CapsuleCollider() {
	}
	
	public CapsuleCollider(Vec3f pos, float height, float radius) {
		this.height = height;
		this.radius = radius;
		this.pos = pos;
		this.localCentroid = pos.add(new Vec3f(0, radius + height*0.5F, 0));
		box = new AxisAlignedBB(-radius, 0, -radius, radius, height+radius*2, radius).offset(pos);
	}
	
	public CapsuleCollider(Vec3f pos, float height, float radius, float density) {
		this(pos, height, radius);
		mass = volume()*density;
		
		//https://www.gamedev.net/tutorials/programming/math-and-physics/capsule-inertia-tensor-r3856/
		//That was some math. I can see why bullet physics did an approximation with cubes.
		final float PI = (float)Math.PI;
		float oneThird = 1F/3F;
		float oneFifth = 1F/5F;
		float oneTwelfth = 1F/12F;
		float rSq = radius*radius;
		float hSq = height*height;
		float halfHeight = height*0.5F;
		//Mass cylinder
		float mcy = height*rSq*PI;
		//Mass hemisphere
		float mhs = 2*rSq*radius*PI*oneThird;
		localInertiaTensor = new Matrix3f();
		localInertiaTensor.m22 = localInertiaTensor.m00 = mcy*(hSq*oneTwelfth + rSq*0.25F) + 2*mhs*(2*rSq*oneFifth + halfHeight*halfHeight + 3*height*radius*0.0125F);
		localInertiaTensor.m11 = mcy*(rSq*0.5F) + 2*mhs*(2*rSq*oneFifth);
	}
	
	public float volume(){
		float fourThirds = 4F/3F;
		//Calculate the volume of the sphere made by the ends of the capsule
		float sVol = fourThirds*(float)Math.PI*radius*radius*radius;
		//Calculate the volume of the cylinder between the two ends
		float cVol = (float)Math.PI*radius*radius*height;
		return sVol + cVol;
	}
	
	@Override
	public Vec3f support(Vec3f direction) {
		//Support point on line segment plus the support function on the sphere.
		if(Math.abs(direction.lenSq()-1) > 0.0001){
			direction = direction.normalize();
		}
		return (direction.y < 0 ? localCentroid.subtract(0, height*0.5F, 0) : localCentroid.add(0, height*0.5F, 0)).add(direction.scale(radius));
	}

	@Override
	public Collider copy() {
		CapsuleCollider c = new CapsuleCollider();
		c.pos = pos;
		c.height = height;
		c.radius = radius;
		c.box = box;
		c.localCentroid = localCentroid;
		c.localInertiaTensor = localInertiaTensor;
		c.mass = mass;
		return c;
	}

	//There has to be a more efficient way to do this, but I don't feel like finding it.
	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		float halfHeight = height*0.5F;
		//Quick check to see if line intersects the other line at all. Possibly improve so the algorithm caps the lines as well?
		float lineToLine = MathHelper.lineDistToLine(pos1, pos2, localCentroid.subtract(0, halfHeight, 0), localCentroid.add(0, halfHeight, 0));
		if(lineToLine > radius){
			return new RayTraceResult();
		}
		Vec3f rayDir = pos2.subtract(pos1);
		float[] cylinder = MathHelper.rayCylinder(pos1, rayDir, localCentroid, radius);
		float[] lowerSphere = MathHelper.raySphere(pos1, rayDir, localCentroid.subtract(0, halfHeight, 0), radius);
		float[] upperSphere = MathHelper.raySphere(pos1, rayDir, localCentroid.add(0, halfHeight, 0), radius);
		Vec3f cPos1 = cylinder != null ? pos1.add(rayDir.scale(cylinder[0])).subtract(localCentroid) : null;
		Vec3f cPos2 = cylinder != null ? pos1.add(rayDir.scale(cylinder[1])).subtract(localCentroid) : null;
		Vec3f sPos1 = lowerSphere != null ? pos1.add(rayDir.scale(lowerSphere[0])).subtract(localCentroid) : null;
		Vec3f sPos2 = lowerSphere != null ? pos1.add(rayDir.scale(lowerSphere[1])).subtract(localCentroid) : null;
		Vec3f sPos3 = upperSphere != null ? pos1.add(rayDir.scale(upperSphere[0])).subtract(localCentroid) : null;
		Vec3f sPos4 = upperSphere != null ? pos1.add(rayDir.scale(upperSphere[1])).subtract(localCentroid) : null;
		float[] hits = new float[6];
		int index = 0;
		if(cylinder != null){
			if(cPos1.y < halfHeight && cPos1.y > -halfHeight){
				hits[index] = cylinder[0];
				index++;
			}
			if(cPos2.y < halfHeight && cPos2.y > -halfHeight){
				hits[index] = cylinder[1];
				index++;
			}
		}
		if(lowerSphere != null){
			if(sPos1.y <= -halfHeight){
				hits[index] = lowerSphere[0];
				index++;
			}
			if(sPos2.y <= -halfHeight){
				hits[index] = lowerSphere[1];
				index++;
			}
		}
		if(upperSphere != null){
			if(sPos3.y >= halfHeight){
				hits[index] = upperSphere[0];
				index++;
			}
			if(sPos4.y >= halfHeight){
				hits[index] = upperSphere[1];
				index++;
			}
		}
		float t1 = Float.MAX_VALUE;
		float t2 = -Float.MAX_VALUE;
		for(int i = 0; i < index; i ++){
			if(hits[i] < t1){
				t1 = hits[i];
			}
		}
		for(int i = 0; i < index; i ++){
			if(hits[i] > t2){
				t2 = hits[i];
			}
		}
		if(t1 < 0){
			t1 = t2;
		}
		if(t1 < 0 || t1 > 1){
			return new RayTraceResult();
		}
		Vec3f pos = pos1.add(rayDir.scale(t1));
		Vec3f normal;
		float checkY = pos.y-localCentroid.y;
		if(checkY > halfHeight){
			normal = pos1.subtract(localCentroid).subtract(0, halfHeight, 0).normalize();
		} else if(checkY < halfHeight){
			normal = pos1.subtract(localCentroid).add(0, halfHeight, 0).normalize();
		} else {
			Vec3f n = pos1.subtract(localCentroid);
			normal = new Vec3f(n.x, 0, n.z).normalize();
		}
		
		return new RayTraceResult(true, t1, pos, normal);
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return box;
	}
	
	@Override
	public void debugRender() {
		RenderHelper.drawCapsule(localCentroid, height, radius, 12);
		RenderHelper.drawBoundingBox(box);
	}

}
