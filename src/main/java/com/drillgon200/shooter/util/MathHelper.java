package com.drillgon200.shooter.util;

public class MathHelper {
	
    public static float wrapDegrees(float value){
        value = value % 360.0F;
        if (value >= 180.0F){
            value -= 360.0F;
        }
        if (value < -180.0F){
            value += 360.0F;
        }
        return value;
    }

    public static double wrapDegrees(double value){
        value = value % 360.0D;
        if(value >= 180.0D){
            value -= 360.0D;
        }
        if(value < -180.0D){
            value += 360.0D;
        }
        return value;
    }
    
    public static double clamp(double val, double a, double b){
    	if(val < a){
    		val = a;
    	}
    	if(val > b){
    		val = b;
    	}
    	return val;
    }
    
    public static float clamp(float val, float a, float b){
    	if(val < a){
    		val = a;
    	}
    	if(val > b){
    		val = b;
    	}
    	return val;
    }
    
    public static double clamp01(double val){
    	if(val < 0){
    		val = 0;
    	}
    	if(val > 1){
    		val = 1;
    	}
    	return val;
    }
    
    public static float clamp01(float val){
    	if(val < 0){
    		val = 0;
    	}
    	if(val > 1){
    		val = 1;
    	}
    	return val;
    }
    
    public static float remap(float num, float min1, float max1, float min2, float max2){
		return ((num - min1) / (max1 - min1)) * (max2 - min2) + min2;
	}

	public static float remap01(float num, float min1, float max1){
		return (num - min1) / (max1 - min1);
	}
	
	public static float remap01_clamp(float num, float min1, float max1){
		return clamp01((num - min1) / (max1 - min1));
	}
	
	public static Vec3f getEulerAngles(Vec3f vec) {
		float yaw = (float) Math.toDegrees(Math.atan2(vec.x, vec.z));
		float sqrt = (float) Math.sqrt(vec.x * vec.x + vec.z * vec.z);
		float pitch = (float) Math.toDegrees(Math.atan2(vec.y, sqrt));
		return new Vec3f(yaw, pitch, 0);
	}
	
	public static int absMaxIdx(double... numbers){
		int idx = 0;
		double max = -Double.MAX_VALUE;
		for(int i = 0; i < numbers.length; i ++){
			double num = Math.abs(numbers[i]);
			if(num > max){
				idx = i;
				max = num;
			}
		}
		return idx;
	}
	
	public static float[] quadf(float a, float b, float c){
		float discrim = b*b-4*a*c;
		if(discrim < 0){
			return null;
		} else {
			discrim = (float) Math.sqrt(discrim);
			float rcp2a = 1F/(2*a);
			return new float[]{(-b+discrim)*rcp2a, (-b-discrim)*rcp2a};
		}
	}
	
	public static float[] raySphere(Vec3f rayPos, Vec3f rayDir, Vec3f sPos, float radius){
		Vec3f sphereToRay = rayPos.subtract(sPos);
		float a = rayDir.lenSq();
		float b = 2*sphereToRay.dot(rayDir);
		float c = sphereToRay.lenSq() - radius*radius;
		return quadf(a, b, c);
	}
	
	public static float[] rayCylinder(Vec3f rayPos, Vec3f rayDir, Vec3f cPos, float radius){
		//I figure a cylinder is just a sphere but without the y axis part, so hopefully it'll just work if I
		//remove the y part from the ray sphere test I figured out a while ago.
		Vec3f sphereToRay = rayPos.subtract(cPos);
		float a = rayDir.x*rayDir.x+rayDir.z*rayDir.z;
		float b = 2*(sphereToRay.x*rayDir.x+sphereToRay.z*rayDir.z);
		float c = (sphereToRay.x*sphereToRay.x+sphereToRay.z*sphereToRay.z) - radius*radius;
		return quadf(a, b, c);
	}
	
	//https://mathworld.wolfram.com/Line-LineDistance.html
	public static float lineDistToLine(Vec3f p1, Vec3f p2, Vec3f p3, Vec3f p4){
		Vec3f a = p2.subtract(p1);
		Vec3f b = p4.subtract(p3);
		Vec3f c = p3.subtract(p1);
		Vec3f cross = a.cross(b);
		if(cross.lenSq() < 0.00001F){
			return p1.distanceTo(p3);
		}
		float denom = cross.len();
		return Math.abs(c.dot(cross))/denom;
	}
}
