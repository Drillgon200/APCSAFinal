package com.drillgon200.shooter.animation;

import java.util.Map;

import com.drillgon200.shooter.util.Matrix4f;

//Acts as a blend tree to create a final pose
public abstract class AnimationNode {

	public final String name;
	
	public abstract Map<String, Transform> generateTransforms(long time);
	
	public abstract AnimationNode copy();
	
	public AnimationNode(String name) {
		this.name = name;
	}
}
