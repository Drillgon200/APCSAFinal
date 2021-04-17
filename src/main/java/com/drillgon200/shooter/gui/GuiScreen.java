package com.drillgon200.shooter.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.drillgon200.shooter.Keyboard;
import com.drillgon200.shooter.Shooter;

public class GuiScreen {

	//I have never written UI code before, so I have no idea what I'm doing.
	//Don't be surprised when all of this is absolute garbage.
	
	//At least optimization shouldn't matter much here since this is all I'm rendering and I don't have to do any
	//of the expensive world rendering
	
	public List<GuiElement> elements = new ArrayList<>();
	public GuiElementTextBox activeTextBox = null;
	
	public void update(float mX, float mY){
		for(GuiElement e : elements){
			e.update(mX, mY);
			if(mX > e.hitbox.x && mX < e.hitbox.z && mY > e.hitbox.y && mY < e.hitbox.w){
				e.onMouseover(mX, mY);
			}
		}
	}
	
	public void onClick(float mX, float mY){
		for(GuiElement e : elements){
			if(mX > e.hitbox.x && mX < e.hitbox.z && mY > e.hitbox.y && mY < e.hitbox.w){
				e.onClick(mX, mY);
			}
		}
	}
	
	public void onType(int key){
		keyPress(key);
	}
	
	public void keyPress(int key){
		for(GuiElement e : elements){
			e.onKeyTyped(key);
		}
	}
	
	public boolean close(){
		Shooter.displayGui(null);
		return true;
	}
	
	public void drawGuiBackgroundLayer(float width, float height){
		
	}
	
	public void draw(float width, float height){
		drawGuiBackgroundLayer(width, height);
		for(GuiElement e : elements){
			e.draw(width, height);
		}
	}
}
