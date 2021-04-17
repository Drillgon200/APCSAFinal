package com.drillgon200.shooter.gui;

import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.TextureManager;

public class GuiElementButton extends GuiElement {

	public String text;
	public Runnable action;
	public boolean active;
	//Font doesn't scale linearly, so the offsets are set manually. I wish I had known this before designing the other buttons that tried linear scaling.
	public float textOffsetX;
	public float textOffsetY;
	public float fontSize;
	
	public GuiElementButton(GuiScreen screen, float posX, float posY, float width, float scale, String text, boolean active, Runnable action) {
		super(screen, posX, posY, width, scale);
		this.setHitbox(posX, posY+scale*0.1F, this.width*scale+scale*1.6F, this.height-scale*0.2F);
		this.text = text;
		this.active = active;
		this.action = action;
	}
	
	public GuiElementButton setFontOffsetScale(float x, float y, float sc){
		this.textOffsetX = x;
		this.textOffsetY = y;
		this.fontSize = sc;
		return this;
	}
	
	@Override
	public void onClick(float posX, float posY) {
		super.onClick(posX, posY);
		action.run();
	}
	
	@Override
	public void draw(float width, float height) {
		GL11.glPushMatrix();
		float mouseFade = getMouseOverFade();
		
		GL11.glTranslated(posX, (mouseClickedTicks > 0 ? -2 : 0) + posY, 0);
		GL11.glScaled(this.height, this.height, 1);
		float color = mouseFade*0.7F + (1-mouseFade)*1F;
		ShaderManager.color(color, color, color, 1);
		TextureManager.bindTexture(Resources.button0);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float pixel = 1F/256F;
		float u = 110F*pixel;
		RenderHelper.drawGuiRect(0, 0, u, 1, 0, 0, u, 1);
		RenderHelper.drawGuiRect(u, 0, this.width, 1, u, 0, u+pixel, 1);
		RenderHelper.drawGuiRect(u+this.width, 0, 1, 1, u+pixel, 0, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		FontRenderer.setActiveFont(FontRenderer.teko_bold);
		FontRenderer.drawString(text, 0.4F, 0.02F, 0.009F, 0.5F*color, 0, 0, 1);
		Resources.basic_texture.use();
		GL11.glPopMatrix();
	}
}
