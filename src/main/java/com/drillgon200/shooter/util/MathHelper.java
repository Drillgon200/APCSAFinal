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
}
