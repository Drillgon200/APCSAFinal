package com.drillgon200.physics;

import com.drillgon200.shooter.util.Vec3f;

public class RayTraceResult {
	
	public boolean hit;
	public Vec3f hitPos;
	public Vec3f hitNormal;
	public float timeOfImpact;
	
	public RayTraceResult() {
		this(false, -1, null, null);
	}
	
	public RayTraceResult(boolean hit, float toi, Vec3f pos, Vec3f normal) {
		this.hit = hit;
		this.hitPos = pos;
		this.hitNormal = normal;
		this.timeOfImpact = toi;
	}
}
