package com.drillgon200.shooter.animation;

import com.drillgon200.shooter.fileformat.DocumentLoader;
import com.drillgon200.shooter.fileformat.DocumentLoader.DocumentNode;

public class AnimationLoader {
	
	public static AnimationClip load(String path){
		DocumentNode doc = DocumentLoader.parseDocument(path);
		
		return new AnimationClip();
	}
}
