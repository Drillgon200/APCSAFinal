package com.drillgon200.shooter.gui;

import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Vec4f;

public class GuiElement {

	public static final Vec4f MAX_HITBOX = new Vec4f(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
	
	public GuiScreen screen;
	public boolean mousedOver;
	public boolean mouseClicked = false;
	public int mouseClickedTicks = 0;
	public int ticksMousedOver;
	public int mouseoverFadeTicks = 10;
	public float posX;
	public float posY;
	public float width;
	public float height;
	public Vec4f hitbox;
	
	public GuiElement(GuiScreen screen, float posX, float posY, float width, float height) {
		this.screen = screen;
		this.posX = posX;
		this.posY = posY;
		this.width = width;
		this.height = height;
		this.setHitbox(posX, posY, width, height);
	}
	
	public void setHitbox(float x, float y, float width, float height){
		hitbox = new Vec4f(x, y, x+width, y+height);
	}
	
	public void update(float mX, float mY){
		if(Shooter.mouse1Down && mouseClicked){
			mouseClickedTicks ++;
		} else{
			mouseClickedTicks = 0;
		}
		mouseClicked = false;
		if(mousedOver){
			mousedOver = false;
		} else {
			ticksMousedOver --;
			if(ticksMousedOver < 0)
				ticksMousedOver = 0;
		}
		if(mX > hitbox.x && mX < hitbox.z && mY > hitbox.y && mY < hitbox.w){
			onMouseover(mX, mY);
		}
	}
	
	public void onMouseover(float posX, float posY){
		mousedOver = true;
		if(Shooter.mouse1Down){
			mouseClicked = true;
		}
		ticksMousedOver = Math.min(ticksMousedOver + 1, mouseoverFadeTicks);
	}
	
	public float getMouseOverFade(){
		return MathHelper.clamp01((mousedOver ? ticksMousedOver + Shooter.partialTicks : ticksMousedOver - Shooter.partialTicks)/mouseoverFadeTicks);
	}
	
	public void onClick(float posX, float posY){
		
	}
	
	public void onKeyTyped(int key){
		
	}
	
	public void draw(float width, float height){
		
	}
}
