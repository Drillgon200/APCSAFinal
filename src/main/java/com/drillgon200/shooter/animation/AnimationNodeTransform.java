package com.drillgon200.shooter.animation;

import java.util.HashMap;
import java.util.Map;

import com.drillgon200.shooter.util.MathHelper;

//The thing that gets the original transforms
public class AnimationNodeTransform extends AnimationNode {

	public Map<String, Transform> map = new HashMap<>();
	public Animation anim;
	
	public AnimationNodeTransform(String name, Animation anim) {
		super(name);
		this.anim = anim;
	}
	
	@Override
	public Map<String, Transform> generateTransforms(long sysTime) {
		if(anim == null){
			map.clear();
			return map;
		}
		Animation activeAnim = anim;
		int numKeyFrames = activeAnim.anim.numKeyFrames;
		int diff = (int) (sysTime - activeAnim.startTime);
		diff *= activeAnim.speedScale;
		if(diff > activeAnim.anim.length) {
			int diff2 = diff % activeAnim.anim.length;
			switch(activeAnim.endResult.type) {
			case END:
				anim.anim = null;
				//render();
				//TODO implement
				return null;
			case REPEAT:
				activeAnim.startTime = sysTime - diff2;
				break;
			case REPEAT_REVERSE:
				activeAnim.startTime = sysTime - diff2;
				activeAnim.speedScale = -activeAnim.speedScale;
				break;
			case START_NEW:
				activeAnim.cloneStats(activeAnim.endResult.next);
				activeAnim.startTime = sysTime - diff2;
				break;
			case STAY:
				activeAnim.startTime = sysTime - activeAnim.anim.length;
				break;
			}
		}
		diff = (int) (sysTime - activeAnim.startTime);
		if(activeAnim.speedScale < 0)
			diff = activeAnim.anim.length - diff;
		diff *= activeAnim.speedScale;
		float remappedTime = MathHelper.clamp(MathHelper.remap(diff, 0, activeAnim.anim.length, 0, numKeyFrames - 1), 0, numKeyFrames - 1);
		float diffN = MathHelper.remap01_clamp(diff, 0, activeAnim.anim.length);
		int index = (int) remappedTime;
		int first = index;
		int next;
		if(index < numKeyFrames - 1) {
			next = index + 1;
		} else {
			next = first;
		}
		
		
		anim.prevFrame = first;
		return getTransformsWithIndex(MathHelper.fract(remappedTime), first, next, diffN);
	}

	public Map<String, Transform> getTransformsWithIndex(float inter, int firstIndex, int nextIndex, float diffN){
		map.clear();
		anim.anim.keyframesByBone.forEach((name, tr) -> {
			map.put(name, tr[firstIndex].interpolate(tr[nextIndex], inter));
		});
		return map;
	}
	
	@Override
	public AnimationNode copy() {
		return new AnimationNodeTransform(name, anim.copy());
	}

}
