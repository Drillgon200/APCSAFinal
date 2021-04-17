package com.drillgon200.shooter.render;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.physics.TreeObject;
import com.drillgon200.shooter.util.Vec3f;

public class StaticGeometry implements TreeObject<StaticGeometry> {

	public int[] offsetAndCount;
	public int materialIndex;
	public AxisAlignedBB box;
	
	public StaticGeometry() {
	}
	
	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		//I wanted to use OperationNotSupportedException, but apparently that requires you to handle it and RuntimeException doesn't.
		throw new RuntimeException("Can't ray trace a render item");
	}
	@Override
	public AxisAlignedBB getBoundingBox() {
		return box;
	}
	
}
