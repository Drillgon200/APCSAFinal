package com.drillgon200.shooter.model;

import java.nio.ByteBuffer;
import java.util.Map;

import com.drillgon200.shooter.util.Matrix4f;

public class Bone {

	public String name;
	public int index;
	public Matrix4f bindPose;
	public Matrix4f invBindPose;
	public Matrix4f currentTransform = new Matrix4f();
	public Bone[] children;
	
	private Bone(){
	}
	
	public Bone(String name, Matrix4f bindPose, int index, Bone[] children) {
		this.name = name;
		this.bindPose = bindPose;
		this.index = index;
		this.children = children;
	}
	
	public void calcInvBindTransforms(Matrix4f parentBindTransform){
		Matrix4f bindTransform = Matrix4f.mul(parentBindTransform, bindPose, null);
		Matrix4f.invert(bindTransform, invBindPose);
		for(Bone child : children){
			child.calcInvBindTransforms(bindTransform);
		}
	}
	
	public void applyTransform(Map<String, Matrix4f> pose, Matrix4f parentTransform){
		Matrix4f currentLocalTransform = pose.getOrDefault(name, bindPose);
		currentTransform = Matrix4f.mul(parentTransform, currentLocalTransform, null);
		for(Bone child : children){
			child.applyTransform(pose, currentTransform);
		}
		Matrix4f.mul(currentTransform, invBindPose, currentTransform);
	}
	
	public void addTransformToBuffer(ByteBuffer buf){
		buf.position(index*Matrix4f.BYTES_PER_MATRIX);
		currentTransform.store(buf);
		for(Bone child : children){
			child.addTransformToBuffer(buf);
		}
	}
	
	//Each instance of this model used requires its own copy so pose data can be kept through the entire frame without recalculation
	public Bone copy(){
		Bone bone = new Bone();
		bone.bindPose = this.bindPose;
		bone.invBindPose = this.invBindPose;
		bone.index = index;
		bone.children = new Bone[children.length];
		for(int i = 0; i < children.length; i ++){
			bone.children[i] = children[i].copy();
		}
		return bone;
	}
	
}
