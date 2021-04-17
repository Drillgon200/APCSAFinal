package com.drillgon200.shooter.render;

import java.nio.ByteBuffer;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.physics.TreeObject;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Vec3f;

public class Light implements TreeObject<Light> {

	public static final int BYTES_PER_LIGHT = 32;
	public static final float cutoff = 0.15F;
	
	private AxisAlignedBB box;
	private LightType type;
	private Vec3f pos;
	private Vec3f color;
	private Vec3f direction;
	private int directionTypeEncoded = 0;
	private int colorEncoded;
	private int spotAnglesPlusShadowIdEncoded = 0;
	private float energy;
	private float radius;
	private float innerAngle;
	private float outerAngle;
	//Set when the lights are sent to the GPU
	private int index = -1;
	
	public Light() {
	}
	
	public Light pointlight(Vec3f pos, Vec3f color, float energy){
		this.type = LightType.POINT;
		this.pos = pos;
		setColor(color);
		setPower(energy);
		recalcBox();
		return this;
	}
	
	public Light spotlight(Vec3f pos, Vec3f dir, Vec3f color, float energy, float innerAngle, float outerAngle){
		this.type = LightType.SPOT;
		this.pos = pos;
		setDirection(dir);
		setColor(color);
		setPower(energy);
		setInnerOuterAngle(innerAngle, outerAngle);
		recalcBox();
		return this;
	}
	
	public Light sunlight(Vec3f dir, Vec3f color, float energy){
		this.type = LightType.SUN;
		this.pos = new Vec3f(0, 0, 0);
		setDirection(dir);
		setColor(color);
		setPower(energy);
		this.radius = Float.MAX_VALUE;
		recalcBox();
		return this;
	}
	
	public boolean setPos(Vec3f newPos){
		if(type == LightType.SUN){
			return false;
		}
		pos = newPos;
		recalcBox();
		return true;
	}
	
	public void setPower(float energy){
		this.energy = energy;
		this.radius = (float) Math.sqrt(Math.max(this.energy/cutoff-1, 0));
		recalcBox();
	}
	
	public float getPower(){
		return energy;
	}
	
	public Vec3f getPos(){
		return pos;
	}
	
	public Vec3f getColor(){
		return color;
	}
	
	public void setColor(Vec3f color){
		colorEncoded = MathHelper.encodeRGBA(color, 0);
		this.color = color;
	}
	
	public float getRadius(){
		return radius;
	}
	
	public void setCustomRadius(float rad){
		this.radius = rad;
		recalcBox();
	}
	
	public Vec3f getDirection(){
		return direction;
	}
	
	/**
	 * 
	 * @param inner - inner angle in radians
	 * @param outer - outer angle in radians
	 * @return
	 */
	public boolean setInnerOuterAngle(float inner, float outer){
		if(type != LightType.SPOT){
			return false;
		}
		this.innerAngle = inner;
		this.outerAngle = outer;
		//We multiply by 0.5 because we need the half angle and input is the full angle, up to half a circle
		float cosInner = (float) Math.cos(innerAngle*0.5F);
		float cosOuter = (float) Math.cos(outerAngle*0.5F);
		int in = (int)(cosInner*MathHelper.tenBits);
		int out = (int)(cosOuter*MathHelper.tenBits);
		spotAnglesPlusShadowIdEncoded = in | (out << 10);
		return true;
	}

	public boolean setDirection(Vec3f dir){
		if(type == LightType.POINT){
			return false;
		}
		this.direction = dir;
		this.directionTypeEncoded = MathHelper.encode2101010Normal(direction, (byte)type.ordinal());
		recalcBox();
		return true;
	}
	
	private void recalcBox(){
		switch(type){
		case POINT:
			box = new AxisAlignedBB(-radius, -radius, -radius, radius, radius, radius).offset(pos);
			break;
		case SPOT:
			Vec3f tangent;
			if(Math.abs(direction.x) >= 0.57735){
				tangent = new Vec3f(direction.y, -direction.x, 0).normalize();
			} else {
				tangent = new Vec3f(0, direction.z, -direction.y).normalize();
			}
			Vec3f bitangent = direction.cross(tangent);
			Matrix3f matrix = new Matrix3f(tangent, direction, bitangent);
			float coneRad = (float) (radius*Math.tan(outerAngle*0.5F));
			box = new AxisAlignedBB(-coneRad, 0, -coneRad, coneRad, radius, coneRad).rotate(matrix).offset(pos);
			break;
		case SUN:
			box = AxisAlignedBB.MAX_EXTENT_AABB;
			break;
		}
	}
	
	public void setIndex(int i){
		this.index = i;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void addToBuffer(ByteBuffer buf){
		//Doing my best to keep this compressed into 2 vec4s.
		buf.putFloat(pos.x);
		buf.putFloat(pos.y);
		buf.putFloat(pos.z);
		buf.putFloat(radius);
		buf.putInt(directionTypeEncoded);
		buf.putInt(colorEncoded);
		buf.putInt(spotAnglesPlusShadowIdEncoded);
		buf.putFloat(energy);
	}

	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		Vec3f dir = pos2.subtract(pos1);
		float[] trace = MathHelper.raySphere(pos1, dir, pos, radius);
		if(trace == null){
			return new RayTraceResult();
		} else {
			Vec3f pos = pos1.add(dir.scale(trace[0]));
			return new RayTraceResult(true, trace[0], pos, pos.subtract(this.pos).normalize());
		}
	}
	
	@Override
	public AxisAlignedBB getBoundingBox() {
		return box;
	}
	
	public static enum LightType {
		POINT,
		SPOT,
		SUN;
	}
}
