package com.drillgon200.shooter;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

public class Keybindings {

	public static Map<Integer, Keybinding> bindings = new HashMap<>();
	
	public static Keybinding forward;
	public static Keybinding back;
	public static Keybinding left;
	public static Keybinding right;
	public static Keybinding jump;
	public static Keybinding sneak;
	
	public static void registerDefaultBindings(){
		forward = registerBinding(new Keybinding("move_forward", GLFW.GLFW_KEY_W));
		back = registerBinding(new Keybinding("move_back", GLFW.GLFW_KEY_S));
		left = registerBinding(new Keybinding("move_left", GLFW.GLFW_KEY_A));
		right = registerBinding(new Keybinding("move_right", GLFW.GLFW_KEY_D));
		jump = registerBinding(new Keybinding("move_right", GLFW.GLFW_KEY_SPACE));
		sneak = registerBinding(new Keybinding("move_right", GLFW.GLFW_KEY_LEFT_SHIFT));
	}
	
	public static Keybinding registerBinding(Keybinding k){
		bindings.put(k.key, k);
		return k;
	}
	
	public static void updateBindings(){
		for(Keybinding k : bindings.values()){
			if(k.isDown){
				k.downTime ++;
			} else {
				k.downTime = 0;
			}
		}
	}
	
	public static class Keybinding {
		public String name;
		public int key;
		public boolean isDown = false;
		public int downTime = 0;
		
		public Keybinding(String name, int key) {
			this.name = name;
			this.key = key;
		}
	}
	
}
