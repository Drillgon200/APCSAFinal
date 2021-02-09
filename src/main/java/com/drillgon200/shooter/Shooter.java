package com.drillgon200.shooter;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.Contact;
import com.drillgon200.physics.ConvexMeshCollider;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.util.Framebuffer;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Project;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Triangle;
import com.drillgon200.shooter.util.Vec3f;
import com.drillgon200.shooter.util.VertexFormat;

public class Shooter {

	public static long window;
	public static int displayWidth = 600;
	public static int displayHeight = 400;
	public static int windowPosX = 0;
	public static int windowPosY = 0;
	public static Framebuffer framebuffer;
	public static Framebuffer postFramebuffer;
	public static boolean mouseGrabbed = false;
	public static long lastTickTime = -1;
	public static float partialTicks = 0;
	
	public static World world;
	
	public static Player player;
	
	//https://www.lwjgl.org/guide
	public static void init(){
		GLFWErrorCallback.createPrint(System.err).set();
		if(!GLFW.glfwInit()){
			throw new RuntimeException("Unable to initialize GLFW");
		}
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		
		window = GLFW.glfwCreateWindow(displayWidth, displayHeight, "tester12345", 0, 0);
		if(window == 0){
			throw new RuntimeException("Failed to create window");
		}
		
		GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE){
				mouseGrabbed = !mouseGrabbed;
				if(mouseGrabbed){
					GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
					int centerX = windowPosX + displayWidth/2;
					int centerY = windowPosY + displayHeight/2;
					GLFW.glfwSetCursorPos(window, centerX, centerY);
				} else {
					GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
				}
				return;
			}
			if(Keybindings.bindings.containsKey(key)){
				if(action == GLFW.GLFW_PRESS){
					Keybindings.bindings.get(key).isDown = true;
				} else if(action == GLFW.GLFW_RELEASE){
					Keybindings.bindings.get(key).isDown = false;
				}
			}
		});
		GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
			if(action == 1 && button == 0){
			}
		});
		
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);
			
			GLFW.glfwGetWindowSize(window, pWidth, pHeight);
			
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			GLFW.glfwSetWindowPos(window, (vidmode.width()-pWidth.get(0))/2, (vidmode.height()-pHeight.get(0))/2);
		}
		
		if(GLFW.glfwRawMouseMotionSupported()){
			GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
		}
		
		GLFW.glfwMakeContextCurrent(window);
		//VSYNC
		GLFW.glfwSwapInterval(1);
		GLFW.glfwShowWindow(window);
		GL.createCapabilities();
		
		GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
		GL11.glEnable(GL13.GL_MULTISAMPLE);
		
		Tessellator.init();
		Resources.reload();
		
		Keybindings.registerDefaultBindings();
		
		world = new World();
		
		player = new Player(world, 0, 4, 0);
		world.addEntity(player);
	}
	
	private static void updateDisplayInfo(){
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer x = stack.mallocInt(1);
			IntBuffer y = stack.mallocInt(1);
			
			GLFW.glfwGetWindowSize(window, x, y);
			displayWidth = x.get(0);
			displayHeight = y.get(0);
			GLFW.glfwGetWindowPos(window, x, y);
			windowPosX = x.get(0);
			windowPosY = y.get(0);
		}
	}
	
	private static void recreateFramebuffer(){
		if(framebuffer == null){
			framebuffer = new Framebuffer(displayWidth, displayHeight, true, false, true);
			postFramebuffer = new Framebuffer(displayWidth, displayHeight, true, false, false);
		} else if(framebuffer.width != displayWidth || framebuffer.height != displayHeight){
			framebuffer.deleteFBO();
			framebuffer = new Framebuffer(displayWidth, displayHeight, true, false, true);
			postFramebuffer.deleteFBO();
			postFramebuffer = new Framebuffer(displayWidth, displayHeight, true, false, false);
		}
	}
	
	//public static float duckCount = 1;
	
	public static void renderLoop() throws Exception{
		lastTickTime = System.currentTimeMillis();
		while(!GLFW.glfwWindowShouldClose(window)){
			updateDisplayInfo();
			mouseInput();
			if(displayWidth <= 0 || displayHeight <= 0){
				GLFW.glfwPollEvents();
				continue;
			}
			long time = System.currentTimeMillis();
			float ticksPassed = ((float)(time-lastTickTime)/MainConfig.TICKLENGTH);
			int ticks = (int)ticksPassed;
			partialTicks = ticksPassed-ticks;
			for(int i = 0; i < Math.min(10, ticks); i ++){
				lastTickTime = time;
				try {
					clientTick();
				} catch(Exception x){
					x.printStackTrace();
					throw new Exception("Exception in main tick loop", x);
				}
			}
			recreateFramebuffer();
			GL11.glEnable(GL32.GL_MULTISAMPLE);
			framebuffer.bindFramebuffer(true);
			framebuffer.clear();
			//render
			/*Resources.blit.use();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			TextureManager.bindTexture(Resources.duck_test);
			//RenderHelper.drawFullscreenTriangle();
			Tessellator tes = Tessellator.instance;
			tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
			float size = 2F/(float)duckCount;
			for(int x = 0; x < duckCount; x ++){
				for(int y = 0; y < duckCount; y ++){
					float xPos = (x*size)-1;
					float yPos = (y*size)-1;
					tes.pos(xPos, yPos, 0).tex(0, 0).endVertex();
					tes.pos(xPos+size, yPos, 0).tex(1, 0).endVertex();
					tes.pos(xPos+size, yPos+size, 0).tex(1, 1).endVertex();
					tes.pos(xPos, yPos+size, 0).tex(0, 1).endVertex();
				}
			}
			tes.draw();
			GL11.glDisable(GL11.GL_BLEND);
			ShaderManager.releaseShader();*/
			
			setupView();
			renderWorld();
			
			framebuffer.unbindFramebuffer();
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer.fbo);
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, postFramebuffer.fbo);
			GL30.glBlitFramebuffer(0, 0, displayWidth, displayHeight, 0, 0, displayWidth, displayHeight, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			Resources.blit.use();
			postFramebuffer.blit();
			ShaderManager.releaseShader();
			
			checkGLError();
			GLFW.glfwSwapBuffers(window);
			GLFW.glfwPollEvents();
		}
	}
	
	private static void setupView(){
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		Project.gluPerspective(70, (float)displayWidth/(float)displayHeight, 0.05F, 1000);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glRotated(player.rotationPitch, 1, 0, 0);
		GL11.glRotated(player.rotationYaw, 0, 1, 0);
		Vec3f pos = player.getInterpolatedPos(partialTicks);
		GL11.glTranslated(-pos.x, -pos.y-player.getEyeHeight(), -pos.z);
	}
	
	private static void renderWorld(){
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glPushMatrix();
		Tessellator tes = Tessellator.instance;
		Resources.world.use();
		TextureManager.bindTexture(Resources.cc0_gravel);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		for(int i = 0; i < 15; i ++){
			float offset = 20*i;
			tes.pos(-10, 0, 10+offset).tex(0, 1).endVertex();
			tes.pos(10, 0, 10+offset).tex(1, 1).endVertex();
			tes.pos(10, 0, -10+offset).tex(1, 0).endVertex();
			tes.pos(-10, 0, -10+offset).tex(0, 0).endVertex();
		}
		tes.draw();
		TextureManager.bindTexture(Resources.duck_test);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(1, 0, -1).tex(0, 0).endVertex();
		tes.pos(0, 0, -3).tex(1, 0).endVertex();
		tes.pos(0, 3, -3).tex(1, 1).endVertex();
		tes.pos(1, 3, -1).tex(0, 1).endVertex();
		tes.draw();
		GL11.glDisable(GL11.GL_BLEND);
		
		Resources.red.use();
		player.body.renderDebugInfo(new Vec3f(0, 0, 0), partialTicks);
		
		ShaderManager.releaseShader();
		GL11.glPopMatrix();
	}
	
	private static void mouseInput(){
		if(mouseGrabbed){
			try(MemoryStack stack = MemoryStack.stackPush()){
				DoubleBuffer x = stack.mallocDouble(1);
				DoubleBuffer y = stack.mallocDouble(1);
				GLFW.glfwGetCursorPos(window, x, y);
				
				int centerX = windowPosX + displayWidth/2;
				int centerY = windowPosY + displayHeight/2;
				
				double dX = x.get(0) - centerX;
				double dY = y.get(0) - centerY;
				dX *= 0.1;
				dY *= 0.1;
				player.rotationYaw = MathHelper.wrapDegrees(player.rotationYaw + dX);
				player.rotationPitch = MathHelper.clamp(player.rotationPitch+dY, -90, 90);
				player.lookVec = new Vec3f((float)Math.toRadians(-player.rotationYaw-90), (float)Math.toRadians(player.rotationPitch+180));
				player.rightVec = new Vec3f((float)Math.toRadians(-player.rotationYaw), 0);
				player.leftVec = player.rightVec.negate();
				player.forwardVec = new Vec3f((float)Math.toRadians(-player.rotationYaw+90), 0);
				player.backVec = player.forwardVec.negate();
				
				GLFW.glfwSetCursorPos(window, centerX, centerY);
			}
		}
	}
	
	private static void checkGLError(){
		int er = GL11.glGetError();
		if(er != 0){
			System.out.println("GL ERROR: " + er);
		}
	}
	
	private static void clientTick(){
		Keybindings.updateBindings();
		float maxWalk = 12;
		float mult = 8;
		if(Keybindings.sneak.isDown){
			maxWalk *= 0.25F;
		}
		if(!player.isOnGround){
			mult = 0.5F;
		}
		if(Keybindings.jump.isDown && player.isOnGround){
			player.motionY += 8;
			mult *= 1.5F;
			maxWalk *= 20;
		}
		if(Keybindings.forward.isDown){
			float amount = MathHelper.clamp(maxWalk-player.getVelocity().dot(player.forwardVec), 0, 1);
			player.addVelocity(player.forwardVec.scale(amount*mult));
		}
		if(Keybindings.back.isDown){
			float amount = MathHelper.clamp(maxWalk-player.getVelocity().dot(player.backVec), 0, 1);
			player.addVelocity(player.backVec.scale(amount*mult));
		}
		if(Keybindings.right.isDown){
			float amount = MathHelper.clamp(maxWalk-player.getVelocity().dot(player.rightVec), 0, 1);
			player.addVelocity(player.rightVec.scale(amount*mult*0.6F));
		}
		if(Keybindings.left.isDown){
			float amount = MathHelper.clamp(maxWalk-player.getVelocity().dot(player.leftVec), 0, 1);
			player.addVelocity(player.leftVec.scale(amount*mult*0.6F));
		}
		if(player.isOnGround){
			player.body.linearDrag = 0.9999F;
		} else {
			player.body.linearDrag = 0.1F;
		}
		world.updateEntities();
	}
}
