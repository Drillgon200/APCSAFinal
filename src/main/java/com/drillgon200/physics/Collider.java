package com.drillgon200.physics;

import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Vec3f;

public abstract class Collider implements TreeObject<Collider> {

	public float mass;
	public Matrix3f localInertiaTensor;
	public Vec3f localCentroid;
	
	public abstract Vec3f support(Vec3f direction);
	
	public abstract Collider copy();
	
	public abstract void debugRender();
}
