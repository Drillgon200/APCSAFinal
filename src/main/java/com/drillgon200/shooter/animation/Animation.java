package com.drillgon200.shooter.animation;

public class Animation {

	public AnimationClip anim;
	public EndResult endResult;
	public float speedScale = 1;
	public long prevFrameTime;
	public long startTime;
	public int prevFrame;
	
	public Animation(AnimationClip a, long startTime, EndResult end) {
		this.anim = a;
		this.prevFrameTime = System.currentTimeMillis();
		this.startTime = startTime;
		this.prevFrame = 0;
	}
	
	public Animation(AnimationClip a, long startTime) {
		this(a, startTime, EndResult.END);
	}
	
	public Animation copy(){
		return new Animation(anim, startTime);
	}
	
	public Animation cloneStats(Animation other){
		this.anim = other.anim;
		this.startTime = other.startTime;
		this.speedScale = other.speedScale;
		this.endResult = other.endResult;
		return this;
	}
	
	public Animation cloneStatsWithoutTime(Animation other){
		this.anim = other.anim;
		this.speedScale = other.speedScale;
		this.endResult = other.endResult;
		return this;
	}
	
	public enum EndType {
		END,
		REPEAT,
		REPEAT_REVERSE,
		START_NEW,
		STAY;
	}
	
	public static class EndResult {
		
		public static final EndResult END = new EndResult(EndType.END, null);
		
		public EndType type;
		public Animation next;
		
		public EndResult(EndType type) {
			this(type, null);
		}
		
		public EndResult(EndType type, Animation next) {
			this.type = type;
			this.next = next;
		}
		
	}
}
