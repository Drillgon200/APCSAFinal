package com.drillgon200.physics;

public interface TreeConsumer<T> {

	/**
	 * @param object - current object being iterated over in the tree
	 */
	public void accept(T object);
	
	/**
	 * @param the box to check against
	 * @return true if it should continue on this branch, false to skip it
	 */
	public boolean shouldContinue(AxisAlignedBB box);
}
