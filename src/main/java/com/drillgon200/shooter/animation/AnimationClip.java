package com.drillgon200.shooter.animation;

import java.util.HashMap;
import java.util.Map;

//The actual animation data
public class AnimationClip {

	public Map<String, Transform[]> keyframesByBone = new HashMap<>();
	public int numKeyFrames;
	public int length;
}
