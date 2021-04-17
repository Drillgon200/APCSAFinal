package com.drillgon200.shooter.animation;

import java.util.Map;

import com.drillgon200.shooter.util.FloatSupplier;

public class AnimationNodeMix extends AnimationNode {

	public FloatSupplier amount;
	public AnimationNode child1;
	public AnimationNode child2;
	
	public AnimationNodeMix(String name, AnimationNode child1, AnimationNode child2, FloatSupplier f) {
		super(name);
		amount = f;
		this.child1 = child1;
		this.child2 = child2;
	}
	
	@Override
	public Map<String, Transform> generateTransforms(long time) {
		float amt = amount.supply();
		if(amt == 1){
			return child1.generateTransforms(time);
		} else if(amt == 0){
			return child2.generateTransforms(time);
		} else{
			//TODO maybe find a better way to do this?
			Map<String, Transform> t1 = child1.generateTransforms(time);
			Map<String, Transform> t2 = child2.generateTransforms(time);
			t1.forEach((name, tr) -> {
				tr.mutateInterpolate(Transform.IDENTITY, 1-amt);
			});
			t2.forEach((name, tr) -> {
				tr.mutateInterpolate(Transform.IDENTITY, amt);
			});
			t1.forEach((name, tr) -> {
				Transform tran = t2.get(name);
				if(tran != null)
					tr.mutateAdd(tran, 1);
			});
			return t1;
		}
	}

	@Override
	public AnimationNode copy() {
		return new AnimationNodeMix(name, child1.copy(), child2.copy(), amount);
	}

}
