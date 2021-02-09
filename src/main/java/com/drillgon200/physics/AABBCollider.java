package com.drillgon200.physics;

import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.Vec3f;

public class AABBCollider extends Collider {

	public AxisAlignedBB box;
	public float density = -1;
	
	//Only use if this is a static collider.
	public AABBCollider(AxisAlignedBB box) {
		this.box = box;
		this.localCentroid = box.getCenter();
	}
	
	public AABBCollider(AxisAlignedBB box, float density) {
		this.box = box;
		float w = (float) (box.maxX-box.minX);
		float h = (float) (box.maxY-box.minY);
		float d = (float) (box.maxZ-box.minZ);
		float vol = w*h*d;
		this.mass = density*vol;
		this.localCentroid = box.getCenter();
		//https://en.wikipedia.org/wiki/List_of_moments_of_inertia
		float i_mass = mass/12F;
		this.localInertiaTensor = new Matrix3f(
				i_mass*(h*h+d*d), 0, 0,
				0, i_mass*(w*w+d*d), 0,
				0, 0, i_mass*(w*w+h*h));
	}

	@Override
	public Vec3f support(Vec3f direction) {
		return new Vec3f(
				direction.x > 0 ? box.maxX : box.minX,
				direction.y > 0 ? box.maxY : box.minY,
				direction.z > 0 ? box.maxZ : box.minZ);
	}

	@Override
	public Collider copy() {
		if(density == -1){
			return new AABBCollider(box);
		} else {
			return new AABBCollider(box, density);
		}
	}

	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		return box.rayCastAABB(pos1, pos2.subtract(pos1));
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return box;
	}

	@Override
	public void debugRender() {
		RenderHelper.drawBoundingBox(box);
	}
}
