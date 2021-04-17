package com.drillgon200.shooter.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.TextureManager;

public class GuiIngame extends GuiScreen {

	
	public List<GuiElement> pauseElements = new ArrayList<>();
	public boolean paused = false;
	public int fadeInTime = 0;
	public int fadeLength = 10;
	
	public GuiIngame() {
		pauseElements.add(new GuiElementButton(this, 20, 200, 2.5F, 80, "Exit game", true, ()->{
			Shooter.player.connection.isClosed = true;
		}));
	}
	
	@Override
	public void update(float mX, float mY) {
		super.update(mX, mY);
		if(paused)
			for(GuiElement e : pauseElements){
				e.update(mX, mY);
				if(mX > e.hitbox.x && mX < e.hitbox.z && mY > e.hitbox.y && mY < e.hitbox.w){
					e.onMouseover(mX, mY);
				}
			}
		if(paused){
			fadeInTime = Math.min(fadeInTime+1, fadeLength);
		} else {
			fadeInTime = Math.max(fadeInTime-1, 0);
		}
	}
	
	@Override
	public void onClick(float mX, float mY) {
		super.onClick(mX, mY);
		if(paused)
			for(GuiElement e : pauseElements){
				if(mX > e.hitbox.x && mX < e.hitbox.z && mY > e.hitbox.y && mY < e.hitbox.w){
					e.onClick(mX, mY);
				}
			}
	}
	
	@Override
	public void keyPress(int key) {
		super.keyPress(key);
		if(paused)
			for(GuiElement e : pauseElements){
				e.onKeyTyped(key);
			}
		if(key == GLFW.GLFW_KEY_ESCAPE){
			Shooter.setMouseGrabbed(paused);
			paused = !paused;
		}
	}
	
	public float getFade(){
		return 1-MathHelper.clamp((paused ? fadeInTime + Shooter.partialTicks : fadeInTime - Shooter.partialTicks)/fadeLength, 0, 1);
	}
	
	@Override
	public void drawGuiBackgroundLayer(float width, float height) {
		float fade = getFade();
		if(fade >= 1)
			return;
		ShaderManager.color(0.05F, 0.05F, 0.05F, 0.8F*(1-fade));
		TextureManager.bindTexture(Resources.white);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		RenderHelper.drawGuiRect(0, 0, width, height, 0, 0, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		ShaderManager.color(1, 1, 1, 1);
	}
	
	@Override
	public void draw(float width, float height) {
		super.draw(width, height);
		float fade = getFade();
		if(fade >= 1)
			return;
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		
		int index = 0;
		float size = 1F/pauseElements.size();
		float size2 = size/2;
		for(GuiElement child : pauseElements){
			GL11.glPushMatrix();
			float aFade = MathHelper.remap01_clamp(fade, index*size - index == 0 ? 0 : size2, index*size+size);
			GL11.glTranslated(-aFade*(1000+child.posX*10), 0, 0);
			child.draw(width, height);
			GL11.glPopMatrix();
			index ++;
		}
		GL11.glPopMatrix();
	}
}
