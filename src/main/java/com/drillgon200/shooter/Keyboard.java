package com.drillgon200.shooter;

import java.util.ArrayList;
import java.util.List;

public class Keyboard {

	private static final int MAX_KEYS = 512;
	
	public static List<Integer> activeKeys = new ArrayList<>();
	private static boolean[] keys = new boolean[MAX_KEYS];
	private static int[] times = new int[MAX_KEYS];
	
	public static void update(){
		for(int i : activeKeys){
			times[i] ++;
		}
	}
	
	public static boolean isKeyDown(int key){
		if(key >= 0 && key < MAX_KEYS){
			return keys[key];
		}
		return false;
	}
	
	public static void keyPress(int key){
		if(key >= 0 && key < MAX_KEYS){
			keys[key] = true;
			activeKeys.add(key);
		}
	}
	
	public static void keyRelease(int key){
		if(key >= 0 && key < MAX_KEYS){
			keys[key] = false;
			times[key] = 0;
			activeKeys.remove(Integer.valueOf(key));
		}
	}
	
	public static int getDownTicks(int key){
		if(key >= 0 && key < MAX_KEYS){
			return times[key];
		}
		return 0;
	}
}
