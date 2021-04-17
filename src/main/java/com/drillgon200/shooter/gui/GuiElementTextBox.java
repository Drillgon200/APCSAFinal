package com.drillgon200.shooter.gui;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.Keyboard;
import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.TextureManager;

public class GuiElementTextBox extends GuiElement {

	private Consumer<String> action;
	public String ghostText;
	public String text = "";
	private int position = 0;
	public int limit;
	
	public GuiElementTextBox(GuiScreen screen, float posX, float posY, float width, float scale, String ghostText, int limit) {
		this(screen, posX, posY, width, scale, ghostText, limit, null);
		this.setHitbox(posX, posY+scale*0.05F, this.width*scale*0.008F+scale*1.3F, this.height-scale*0.6F);
		this.mouseoverFadeTicks = 5;
	}
	
	public GuiElementTextBox(GuiScreen screen, float posX, float posY, float width, float height, String ghostText, int limit, Consumer<String> action) {
		super(screen, posX, posY, width, height);
		this.ghostText = ghostText;
		this.limit = limit;
		this.action = action;
	}
	
	public void add(char c){
		if(text.length() < limit){
			text = text.substring(0, position) + c + text.substring(position, text.length());
			position ++;
		}
	}
	
	public void remove(){
		if(position == 0)
			return;
		text = text.substring(0, position-1) + text.substring(position, text.length());
		position --;
	}
	
	public void executeAction(){
		if(action != null){
			action.accept(text);
		}
	}
	
	@Override
	public void onClick(float posX, float posY) {
		screen.activeTextBox = this;
		//Set the position and stuff
	}
	
	@Override
	public void update(float mX, float mY) {
		super.update(mX, mY);
		for(int key : Keyboard.activeKeys){
			if(Keyboard.getDownTicks(key) > 25){
				onKeyTyped(key);
			}
		}
	}
	
	@Override
	public void onKeyTyped(int key) {
		char typed = 0;
		if(key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_GRAVE_ACCENT){
			typed = (char)key;
		} else if(key == GLFW.GLFW_KEY_TAB){
			typed = '	';
		}
		if(Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)){
			typed = Character.toUpperCase(typed);
		} else {
			typed = Character.toLowerCase(typed);
		}
		if(screen.activeTextBox == this){
			if(typed > 0){
				this.add(typed);
			} else if(key == GLFW.GLFW_KEY_BACKSPACE){
				this.remove();
			}
		}
	}
	
	@Override
	public void draw(float width, float height) {
		//Draw box
		GL11.glPushMatrix();
		float mouseFade = getMouseOverFade();
		//GL11.glTranslated(mouseFade*-5, mouseClickedTicks > 0 ? -2 : 0, 0);
		float color = mouseFade*1.3F + (1-mouseFade)*1F;
		if(screen.activeTextBox == this)
			color = 1.3F;
		ShaderManager.color(color, color, color, 1F);
		TextureManager.bindTexture(Resources.textBox0);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float pixel = 1F/256F;
		float u = 69F*pixel;
		RenderHelper.drawGuiRect(posX, posY, this.height*u, this.height*0.5F, 0, 0, u, 1);
		RenderHelper.drawGuiRect(posX+this.height*u, posY, this.width, this.height*0.5F, u, 0, u+pixel, 1);
		RenderHelper.drawGuiRect(posX+this.height*u+this.width, posY, this.height, this.height*0.5F, u+pixel, 0, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		
		//Draw string
		FontRenderer.setActiveFont(FontRenderer.teko_bold);
		if(text.length() > 0){
			FontRenderer.drawString(text, posX+20, posY+4, 0.00045F*height, 0.6F, 0, 0, 1);
		} else {
			FontRenderer.drawString(ghostText, posX+20, posY+4, 0.00045F*height, 0.25F, 0.125F, 0F, 1F);
		}
		Resources.basic_texture.use();
		
		//Draw flashy bar
		if(screen.activeTextBox == this && (Shooter.totalTicks/30) % 2 == 0){
			u = 45F*pixel;
			TextureManager.bindTexture(Resources.white);
			ShaderManager.color(0.6F, 0, 0, 1);
			float textWidth = FontRenderer.getStringWidth(text, 0.045F*height);
			RenderHelper.drawGuiRect(posX+this.height*u*0.9F+textWidth, posY+this.height*u*0.75F, this.height*u*0.2F, this.height*0.25F, 0, 0, u, 1);
		}
		
		GL11.glPopMatrix();
	}
}
