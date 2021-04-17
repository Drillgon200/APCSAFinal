package com.drillgon200.shooter.animation;

import com.drillgon200.shooter.util.Matrix4f;
import com.drillgon200.shooter.util.Quat4f;
import com.drillgon200.shooter.util.Vec3f;

public class Transform {

	public static final Transform IDENTITY = new Transform(new Vec3f(1), new Quat4f(), new Vec3f(0));
	
	public Vec3f scale;
	public Quat4f rotation;
	public Vec3f translation;
	
	public Transform(float[] matrix) {
		float scaleX = new Vec3f(matrix[0], matrix[1], matrix[2]).len();
		float scaleY = new Vec3f(matrix[4], matrix[5], matrix[6]).len();
		float scaleZ = new Vec3f(matrix[8], matrix[9], matrix[10]).len();
		
		matrix[0] = matrix[0]/scaleX;
		matrix[1] = matrix[1]/scaleX;
		matrix[2] = matrix[2]/scaleX;
		
		matrix[4] = matrix[4]/scaleY;
		matrix[5] = matrix[5]/scaleY;
		matrix[6] = matrix[6]/scaleY;
		
		matrix[8] = matrix[8]/scaleZ;
		matrix[9] = matrix[9]/scaleZ;
		matrix[10] = matrix[10]/scaleZ;
		
		scale = new Vec3f(scaleX, scaleY, scaleZ);
		rotation = new Quat4f().setFromMat(matrix[0], matrix[1], matrix[2], matrix[4], matrix[5], matrix[6], matrix[8], matrix[9], matrix[10]);
		translation = new Vec3f(matrix[0*4+3], matrix[1*4+3], matrix[2*4+3]);
	}
	
	public Transform(Vec3f scale, Quat4f rot, Vec3f tran) {
		this.scale = scale;
		this.rotation = rot;
		this.translation = tran;
	}
	
	public Transform mutateInterpolate(Transform other, float amount){
		Vec3f sc = scale.lerp(other.scale, amount);
		Quat4f rot = new Quat4f(rotation).interpolate(other.rotation, amount);
		Vec3f tran = translation.lerp(other.translation, amount);
		this.scale = sc;
		this.rotation = rot;
		this.translation = tran;
		return this;
	}
	
	public Transform interpolate(Transform other, float amount){
		return copy().mutateInterpolate(other, amount);
	}
	
	public Transform mutateAdd(Transform other, float amount){
		if(amount == 1){
			Vec3f sc = scale.mul(other.scale);
			Quat4f rot = new Quat4f(rotation).mul(other.rotation);
			Vec3f tran = translation.add(other.translation);
			this.scale = sc;
			this.rotation = rot;
			this.translation = tran;
		} else {
			Vec3f sc = scale.mul(new Vec3f(1).lerp(other.scale, amount));
			Quat4f rot = new Quat4f(rotation).mul(new Quat4f().interpolate(other.rotation, amount));
			Vec3f tran = translation.add(other.translation.scale(amount));
			this.scale = sc;
			this.rotation = rot;
			this.translation = tran;
		}
		return this;
	}
	
	public Transform add(Transform other, float amount){
		return copy().mutateAdd(other, amount);
	}
	
	public Matrix4f toMatrix(){
		Matrix4f mat = new Matrix4f();
		rotation.matrixFromQuat(mat);
		mat.scale(scale);
		mat.m30 = translation.x;
		mat.m31 = translation.y;
		mat.m32 = translation.z;
		mat.m33 = 1;
		return mat;
	}
	
	public Transform copy(){
		return new Transform(scale, rotation, translation);
	}
	
	@Override
	public String toString() {
		return "Transform\nScale: " + scale + "\nQuaternion: " + rotation + "\nTranslation: " + translation;
	}
}
