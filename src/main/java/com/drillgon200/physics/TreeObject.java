package com.drillgon200.physics;

import com.drillgon200.physics.AABBTree.Node;
import com.drillgon200.shooter.util.Vec3f;

public interface TreeObject<T extends TreeObject<T>> {

	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2);
	public AxisAlignedBB getBoundingBox();
	public default void setUserData(Node<T> n){}
	public default Node<T> removeUserData(){return null;}
}
