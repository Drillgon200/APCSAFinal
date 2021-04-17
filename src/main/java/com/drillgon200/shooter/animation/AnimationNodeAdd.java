package com.drillgon200.shooter.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.drillgon200.shooter.util.ConsumerFloatSupplier;

public class AnimationNodeAdd extends AnimationNode {

	public List<AnimationNode> children;
	public ConsumerFloatSupplier<String> scaleGetter;
	
	public AnimationNodeAdd(String name, ConsumerFloatSupplier<String> scaleGetter, AnimationNode... nodes) {
		super(name);
		children = Arrays.asList(nodes);
		this.scaleGetter = scaleGetter;
	}
	
	public AnimationNodeAdd(String name, ConsumerFloatSupplier<String> scaleGetter, List<AnimationNode> c){
		super(name);
		children = c;
		this.scaleGetter = scaleGetter;
	}
	
	@Override
	public Map<String, Transform> generateTransforms(long time) {
		Map<String, Transform> base = children.get(0).generateTransforms(time);
		for(int i = 1; i < children.size(); i ++){
			AnimationNode child = children.get(i);
			Map<String, Transform> add = children.get(i).generateTransforms(time);
			final float scale = scaleGetter != null ? scaleGetter.supply(child.name) : 1;
			add.forEach((name, tran) -> {
				Transform tr = base.get(name);
				if(tr != null){
					tr.mutateAdd(tran, scale);
				} else {
					base.put(name, tran);
				}
			});
		}
		return base;
	}

	@Override
	public AnimationNode copy() {
		List<AnimationNode> newList = new ArrayList<>(children.size());
		for(AnimationNode a : children){
			newList.add(a.copy());
		}
		return new AnimationNodeAdd(name, this.scaleGetter, newList);
	}
}
