package com.drillgon200.shooter.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Vec4f;

public class GuiElementMenuButton extends GuiElement {

	//I feel like this code should be way simpler than it is.
	
	public String text;
	public boolean realMenu;
	public int fadeInTime = 0;
	public GuiElementMenuButton parent;
	public GuiElementMenuButton active = null;
	public GuiElementMenuButton prevActive = null;
	public List<GuiElement> children = new ArrayList<>();
	public int fadeLength = 15;
	public Vec4f menu_hitbox;
	
	public GuiElementMenuButton(GuiScreen screen) {
		super(screen, 0, 0, 0, 0);
		realMenu = false;
		fadeInTime = 20;
		this.hitbox = MAX_HITBOX;
	}
	
	public GuiElementMenuButton(GuiScreen screen, GuiElementMenuButton parent, float posX, float posY, float width, float scale, String text) {
		super(screen, posX, posY, width, scale);
		realMenu = true;
		this.parent = parent;
		this.setHitbox(posX, posY+scale*0.1F, this.width*scale*0.01675F+scale*1.6F, this.height-scale*0.2F);
		menu_hitbox = hitbox;
		this.text = text;
	}
	
	public void setParents(){
		for(GuiElement e : children){
			if(e instanceof GuiElementMenuButton){
				((GuiElementMenuButton) e).setParents();
				((GuiElementMenuButton) e).parent = this;
			}
		}
	}
	
	@Override
	public void update(float mX, float mY) {
		super.update(mX, mY);
		boolean shouldRender = (parent == null || parent.active == this) && active == null;
		
		if(!shouldRender){
			fadeInTime = Math.max(fadeInTime - 1, 0);
		} else {
			if(prevActive != null){
				if(prevActive.fadeInTime == 0){
					fadeInTime = Math.min(fadeInTime + 1, fadeLength);
					prevActive = null;
				}
			} else {
				if(parent == null || parent.fadeInTime == 0)
					fadeInTime = Math.min(fadeInTime + 1, fadeLength);
			}
		}
		for(GuiElement child : children){
			child.update(mX, mY);
		}
	}
	
	@Override
	public void onClick(float mX, float mY) {
		if(active != null){
			active.onClick(mX, mY);
			return;
		}
		for(GuiElement e : children){
			if(mX > e.hitbox.x && mX < e.hitbox.z && mY > e.hitbox.y && mY < e.hitbox.w){
				if(e instanceof GuiElementMenuButton){
					active = (GuiElementMenuButton) e;
					active.hitbox = MAX_HITBOX;
					return;
				} else {
					e.onClick(mX, mY);
				}
			}
		}
	}
	
	@Override
	public void onKeyTyped(int key) {
		if(key == GLFW.GLFW_KEY_ESCAPE){
			if(active != null && active.active == null){
				active.hitbox = active.menu_hitbox;
				prevActive = active;
				active = null;
				screen.activeTextBox = null;
			}
		}
		for(GuiElement e : children){
			e.onKeyTyped(key);
		}
	}
	
	public float getFade(){
		return 1-MathHelper.clamp((active == null ? fadeInTime + Shooter.partialTicks : fadeInTime - Shooter.partialTicks)/fadeLength, 0, 1);
	}
	
	@Override
	public void draw(float width, float height) {
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		float fade = getFade();
		
		int index = 0;
		float size = 1F/children.size();
		float size2 = size/2;
		for(GuiElement child : children){
			GL11.glPushMatrix();
			float aFade = MathHelper.remap01_clamp(fade, index*size - index == 0 ? 0 : size2, index*size+size);
			GL11.glTranslated(-aFade*(1000+child.posX*10), 0, 0);
			child.draw(width, height);
			GL11.glPopMatrix();
			index ++;
		}
		GL11.glPopMatrix();
		if(!realMenu)
			return;
		GL11.glPushMatrix();
		float mouseFade = getMouseOverFade();
		GL11.glTranslated(mouseFade*-5, mouseClickedTicks > 0 ? -2 : 0, 0);
		float color = mouseFade*0.5F + (1-mouseFade)*1F;
		ShaderManager.color(color, color, color, 1);
		TextureManager.bindTexture(Resources.menuButton0);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float pixel = 1F/256F;
		float u = 172F*pixel;
		RenderHelper.drawGuiRect(posX, posY, this.height*u, this.height, 0, 0, u, 1);
		RenderHelper.drawGuiRect(posX+this.height*u, posY, this.width, this.height, u, 0, u+pixel, 1);
		RenderHelper.drawGuiRect(posX+this.height*u+this.width, posY, this.height, this.height, u+pixel, 0, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		FontRenderer.setActiveFont(FontRenderer.teko_bold);
		FontRenderer.drawString(text, posX+40, posY+6, 0.0005F*height, 0.5F*color, 0, 0, 1);
		Resources.basic_texture.use();
		GL11.glPopMatrix();
	}
	
}
