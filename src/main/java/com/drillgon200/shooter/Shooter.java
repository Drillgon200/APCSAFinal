package com.drillgon200.shooter;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import com.drillgon200.networking.tcp.TCPNetworkThreadClient;
import com.drillgon200.networking.udp.UDPNetworkThreadClient;
import com.drillgon200.physics.GJK;
import com.drillgon200.shooter.entity.EntityRegistry;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.entity.PlayerClient;
import com.drillgon200.shooter.entity.PlayerClientMP;
import com.drillgon200.shooter.gui.FontRenderer;
import com.drillgon200.shooter.gui.GuiScreen;
import com.drillgon200.shooter.gui.TitleScreen;
import com.drillgon200.shooter.level.LevelClient;
import com.drillgon200.shooter.level.LevelLoader;
import com.drillgon200.shooter.packets.CPacketPlayerInput;
import com.drillgon200.shooter.render.ClusterManager;
import com.drillgon200.shooter.render.Skybox;
import com.drillgon200.shooter.util.Framebuffer;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.MathHelper;
import com.drillgon200.shooter.util.Matrix4f;
import com.drillgon200.shooter.util.Project;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.Vec3f;

public class Shooter {

	public static final float VERSION = 0.1F;
	
	public static FloatBuffer MATRIX_BUFFER;
	
	public static long window;
	public static float prevFov = 70F;
	public static float fov = 70F;
	public static int prevWidth = 600;
	public static int prevHeight = 400;
	public static int displayWidth = 600;
	public static int displayHeight = 400;
	public static int uiWidth;
	public static int uiHeight;
	public static float uiScaleFactor;
	public static int windowPosX = 0;
	public static int windowPosY = 0;
	public static Framebuffer framebuffer;
	public static Framebuffer postFramebuffer;
	public static boolean mouseGrabbed = false;
	public static float guiMousePosX;
	public static float guiMousePosY;
	
	public static long totalTicks = 0;
	
	public static Skybox skybox;
	
	public static float zNear = 0.05F;
	public static float zFar = 100.0F;
	
	public static long lastTickTime = -1;
	public static float partialTicks = 0;
	public static Matrix4f viewMatrix = new Matrix4f();
	public static Matrix4f inverseViewMatrix = new Matrix4f();
	
	public static GuiScreen hud = null;
	public static GuiScreen currentScreen = null;
	
	private static Queue<Runnable> scheduledTasks = new ArrayDeque<>();
	
	public static TitleScreen titleScreen;
	public static UDPNetworkThreadClient connection;
	
	public static EntityRegistry entityRegistry;
	public static WorldClient world;
	
	public static volatile ConnectionState state = ConnectionState.DISCONNECTED;
	
	public static PlayerClient player;
	
	//TODO replace with keybinding system
	public static boolean mouse1Down = false;
	public static boolean mouse2Down = false;
	
	//https://www.lwjgl.org/guide
	public static void init(){
		System.setProperty("java.awt.headless", "true");
		GLFWErrorCallback.createPrint(System.err).set();
		if(!GLFW.glfwInit()){
			throw new RuntimeException("Unable to initialize GLFW");
		}
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		
		window = GLFW.glfwCreateWindow(displayWidth, displayHeight, "CTF Arena " + VERSION, 0, 0);
		if(window == 0){
			throw new RuntimeException("Failed to create window");
		}
		
		GLFWImage icon = loadGLFWImage("/assets/shooter/cursor_small.png");
		long cursor = GLFW.glfwCreateCursor(icon, 19, 4);
		GLFW.glfwSetCursor(window, cursor);
		
		GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if(action == GLFW.GLFW_PRESS){
				Keyboard.keyPress(key);
			} else if(action == GLFW.GLFW_RELEASE){
				Keyboard.keyRelease(key);
			}
			if(currentScreen != null && action == GLFW.GLFW_PRESS){
				currentScreen.onType(key);
				if(!mouseGrabbed)
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
			if(action == GLFW.GLFW_PRESS){
				if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT){
					mouse1Down = true;
					if(currentScreen != null){
						currentScreen.onClick(guiMousePosX, guiMousePosY);
					}
				}
			}
			if(action == GLFW.GLFW_RELEASE){
				if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT){
					mouse1Down = false;
				}
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
		
		GLFWImage.Buffer buffer = GLFWImage.create(1);
		icon = loadGLFWImage("/assets/shooter/icon.png");
		buffer.put(icon);
		buffer.flip();
		GLFW.glfwSetWindowIcon(window, buffer);
		
		//VSYNC
		GLFW.glfwSwapInterval(1);
		GLFW.glfwShowWindow(window);
		GL.createCapabilities();
		
		GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
		GL11.glEnable(GL13.GL_MULTISAMPLE);
		
		MATRIX_BUFFER = GLAllocation.createDirectFloatBuffer(16);
		
		System.out.println("Initilalizing tessellator...");
		Tessellator.init();
		System.out.println("Finished.");
		checkGLError();
		
		entityRegistry = new EntityRegistry();
		registerEntities();
		
		System.out.println("Initializing clustered forward renderer...");
		ClusterManager.init();
		System.out.println("Finished.");
		checkGLError();
		
		System.out.println("Loading common resources...");
		Resources.reload();
		System.out.println("Finished.");
		checkGLError();
		
		System.out.println("Loading fonts...");
		FontRenderer.init();
		System.out.println("Finished.");
		checkGLError();
		
		System.out.println("Loading Levels...");
		LevelClient level = LevelLoader.loadClient("/assets/shooter/levels/level_test.dlf");
		System.out.println("Finished.");
		checkGLError();
		
		connection = new UDPNetworkThreadClient();
		connection.start();
		
		skybox = new Skybox("/assets/shooter/textures/skybox/space0");
		titleScreen = new TitleScreen();
		displayGui(titleScreen);
		
		//I'm really not sure whether using a margin is better or not
		GJK.margin = 0.0F;
		PacketDispatcher.registerPackets();
		Keybindings.registerDefaultBindings();
		
		world = new WorldClient();
		world.setLevel(level);
	}
	
	public static void registerEntities(){
		entityRegistry.registerEntitiesCommon();
		entityRegistry.register("player_client", PlayerClient.class);
		entityRegistry.register("player_client_multiplayer", PlayerClientMP.class);
	}
	
	public static void shutdown(){
		GL.destroy();
		GLFW.glfwTerminate();
		connection.terminate();
		try {
			connection.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		ShooterServer.shutdown_async(new ArrayDeque<String>());
	}
	
	public static GLFWImage loadGLFWImage(String location){
		try {
			BufferedImage img = ImageIO.read(Shooter.class.getResourceAsStream(location));
			int size = img.getWidth()*img.getHeight()*4;
			ByteBuffer data = GLAllocation.createDirectByteBuffer(size);
			for(int y = 0; y < img.getHeight(); y ++){
				for(int x = 0; x < img.getWidth(); x ++){
					int argb = img.getRGB(x, y);
					data.put((byte) ((argb >> 16) & 255));
					data.put((byte) ((argb >> 8) & 255));
					data.put((byte) (argb & 255));
					data.put((byte) ((argb >> 24) & 255));
				}
			}
			data.rewind();
			GLFWImage icon = GLFWImage.create().set(img.getWidth(), img.getHeight(), data);
			//GLAllocation.freeDirectBuffer(data);
			return icon;
		} catch(Exception x){
			x.printStackTrace();
		}
		return null;
	}
	
	public static void setMouseGrabbed(boolean grab){
		mouseGrabbed = grab;
		if(grab){
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
			int centerX = windowPosX + displayWidth/2;
			int centerY = windowPosY + displayHeight/2;
			GLFW.glfwSetCursorPos(window, centerX, centerY);
		} else {
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
	}
	
	public static void addScheduledTask(Runnable r){
		synchronized(scheduledTasks){
			scheduledTasks.add(r);
		}
	}
	
	private static void updateDisplayInfo(){
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer x = stack.mallocInt(1);
			IntBuffer y = stack.mallocInt(1);
			
			GLFW.glfwGetWindowSize(window, x, y);
			prevWidth = displayWidth;
			prevHeight = displayHeight;
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
	
	public static void updateLoop() throws Exception{
		lastTickTime = System.currentTimeMillis();
		recreateProjection();
		rebuildClusters();
		while(!GLFW.glfwWindowShouldClose(window)){
			updateDisplayInfo();
			mouseInput();
			if(displayWidth <= 0 || displayHeight <= 0){
				GLFW.glfwPollEvents();
				continue;
			}
			recreateProjection();
			if(displayWidth != prevWidth || displayHeight != prevHeight || prevFov != fov){
				rebuildClusters();
			}
			prevFov = fov;
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
			
			if(player != null){
				setupView();
				Vec3f viewTrans = viewMatrix.getTranslation();
				viewMatrix.setTranslation(0, 0, 0);
				skybox.draw();
				viewMatrix.setTranslation(viewTrans);
				renderWorld();
			}
			setupUIRendering();
			renderUI();
			
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
	
	public static void setupUIRendering(){
		uiScaleFactor = ((displayWidth/1920F)+(displayHeight/1080F))*0.5F;
		uiWidth = (int) Math.ceil(displayWidth/uiScaleFactor);
		uiHeight = (int) Math.ceil(displayHeight/uiScaleFactor);
		//GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, uiWidth, 0, uiHeight, 0, 1000);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslated(0, 0, -500);
		viewMatrix.identity();
	}
	
	public static void recreateProjection(){
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		Project.gluPerspective(70, (float)displayWidth/(float)displayHeight, zNear, zFar);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	
	public static void rebuildClusters(){
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, MATRIX_BUFFER);
		ClusterManager.rebuildClusters(zNear, zFar, new Matrix4f().load(MATRIX_BUFFER));
		MATRIX_BUFFER.rewind();
	}
	
	private static void setupView(){
		GL11.glLoadIdentity();
		GL11.glRotated(player.rotationPitch, 1, 0, 0);
		GL11.glRotated(player.rotationYaw, 0, 1, 0);
		Vec3f pos = player.getInterpolatedPos(partialTicks);
		GL11.glTranslated(-pos.x, -pos.y-player.getEyeHeight(), -pos.z);
		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
		viewMatrix = new Matrix4f().load(MATRIX_BUFFER);
		inverseViewMatrix = viewMatrix.copy().invert();
		MATRIX_BUFFER.rewind();
		GL11.glLoadIdentity();
	}
	
	public static void renderWorld(){
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glPushMatrix();
		
		Resources.world.use();
		world.gatherLights();
		ClusterManager.updateLightLists();
		//Materials will possibly use 6 textures, so we'll use texture offset 5 for the light information textures
		int id = Resources.world.getShaderId();
		ClusterManager.sendUniforms(id, GL13.GL_TEXTURE5);
		world.level.sendLightmapUniforms(id, GL13.GL_TEXTURE8);
		GL20.glUniform1i(GL20.glGetUniformLocation(id, "applyStaticLights"), 0);
		Vec3f pos = player.getInterpolatedPos(partialTicks);
		GL20.glUniform3f(GL20.glGetUniformLocation(id, "view_pos"), pos.x, pos.y+player.getEyeHeight(), pos.z);
		//Iterator<Light> itr = world.level.staticLights.iterator();
		//itr.next();
		//System.out.println(itr.next().getPower());
		
		world.renderStaticGeo();
		/*Resources.gravel.sendUniformData(id, "mat.");
		GL20.glUniform1i(GL20.glGetUniformLocation(id, "lightCount"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(id, "lightIndices[0]"), 0);
		Light light = new Light(new Vec3f(0, 1, 0), new Vec3f(1), 1000, 100);
		light.sendUniformData(id, "lights[0]");
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		for(int i = 0; i < 15; i ++){
			float offset = 20*i;
			tes.pos(-10, 0, 10+offset).tex(0, 1).endVertex();
			tes.pos(10, 0, 10+offset).tex(1, 1).endVertex();
			tes.pos(10, 0, -10+offset).tex(1, 0).endVertex();
			tes.pos(-10, 0, -10+offset).tex(0, 0).endVertex();
		}
		tes.pos(-10, 4, 10+20).tex(0, 1).endVertex();
		tes.pos(10, 4, 10+20).tex(1, 1).endVertex();
		tes.pos(10, 0, -10+20).tex(1, 0).endVertex();
		tes.pos(-10, 0, -10+20).tex(0, 0).endVertex();
		
		tes.pos(-10, 0, 10+40).tex(0, 1).endVertex();
		tes.pos(10, 0, 10+40).tex(1, 1).endVertex();
		tes.pos(10, 4, -10+40).tex(1, 0).endVertex();
		tes.pos(-10, 4, -10+40).tex(0, 0).endVertex();
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
		GL11.glDisable(GL11.GL_BLEND);*/
		
		
		Resources.red.use();
		for(Player p : world.playerEntities){
			p.body.renderDebugInfo(new Vec3f(0, 0, 0), partialTicks+(p == player ? 0 : world.ticksSinceLastStateUpdate));
		}
		//world.level.staticColliders.forEach(coll -> coll.debugRender());
		ShaderManager.releaseShader();
		GL11.glPopMatrix();
	}
	
	private static void renderUI(){
		GL11.glPushMatrix();
		Resources.basic_texture.use();
		if(hud != null && world.level != null){
			hud.draw(uiWidth, uiHeight);
		}
		if(currentScreen != null){
			currentScreen.draw(uiWidth, uiHeight);
		}
		//FontRenderer.setActiveFont(FontRenderer.teko_bold);
		//FontRenderer.drawString("CTF", uiWidth/2, uiHeight/2, 0.5F);
		GL11.glPopMatrix();
	}
	
	private static void mouseInput(){
		try(MemoryStack stack = MemoryStack.stackPush()){
				DoubleBuffer x = stack.mallocDouble(1);
				DoubleBuffer y = stack.mallocDouble(1);
				GLFW.glfwGetCursorPos(window, x, y);
				guiMousePosX = MathHelper.clamp((float) (x.get())/uiScaleFactor, 0, uiWidth);
				guiMousePosY = uiHeight - MathHelper.clamp((float) (y.get())/uiScaleFactor, 0, uiHeight);
				
			if(mouseGrabbed){
				int centerX = windowPosX + displayWidth/2;
				int centerY = windowPosY + displayHeight/2;
				
				double dX = x.get(0) - centerX;
				double dY = y.get(0) - centerY;
				dX *= 0.1;
				dY *= 0.1;
				if(player != null){
					player.rotationYaw = MathHelper.wrapDegrees(player.rotationYaw + dX);
					player.rotationPitch = MathHelper.clamp(player.rotationPitch+dY, -90, 90);
					player.lookVec = new Vec3f((float)Math.toRadians(-player.rotationYaw-90), (float)Math.toRadians(player.rotationPitch+180));
					player.rightVec = new Vec3f((float)Math.toRadians(-player.rotationYaw), 0);
					player.leftVec = player.rightVec.negate();
					player.forwardVec = new Vec3f((float)Math.toRadians(-player.rotationYaw+90), 0);
					player.backVec = player.forwardVec.negate();
				}
				
				GLFW.glfwSetCursorPos(window, centerX, centerY);
			}
		}
	}
	
	public static void displayGui(GuiScreen gui){
		currentScreen = gui;
	}
	
	public static void checkGLError(){
		int er = GL11.glGetError();
		if(er != 0){
			System.out.println("GL ERROR: " + er + " " + translateGlError(er));
		}
	}
	
	private static String translateGlError(int err){
		switch(err){
		case GL11.GL_INVALID_ENUM:
			return "GL_INVALID_ENUM";
		case GL11.GL_INVALID_VALUE:
			return "GL_INVALID_VALUE";
		case GL11.GL_INVALID_OPERATION:
			return "GL_INVALID_OPERATION";
		case GL11.GL_STACK_OVERFLOW:
			return "GL_STACK_OVERFLOW";
		case GL11.GL_STACK_UNDERFLOW:
			return "GL_STACK_UNDERFLOW";
		case GL11.GL_OUT_OF_MEMORY:
			return "GL_OUT_OF_MEMORY";
		}
		return "";
	}
	
	private static void clientTick(){
		world.ticksSinceLastStateUpdate ++;
		synchronized(scheduledTasks){
			for(Runnable r : scheduledTasks){
				r.run();
			}
			scheduledTasks.clear();
		}
		Keybindings.updateBindings();
		Keyboard.update();
		if(state == ConnectionState.CONNECTED || state == ConnectionState.DISCONNECTING){
			if((player != null && player.connection.isClosed) || state == ConnectionState.DISCONNECTING){
				player = null;
				world.reset();
				displayGui(titleScreen);
				setMouseGrabbed(false);
				state = ConnectionState.DISCONNECTED;
			}
		}
		if(player != null){
			if(mouseGrabbed){
				playerInput();
			}
			if(player.isOnGround){
				player.body.linearDrag = 0.9999F;
			} else {
				player.body.linearDrag = 0.1F;
			}
		}
		world.updateEntities();
		if(player != null){
			PacketDispatcher.sendToServer(new CPacketPlayerInput(player));
		}
		if(currentScreen != null){
			currentScreen.update(guiMousePosX, guiMousePosY);
		}
		totalTicks ++;
	}
	
	private static void playerInput(){
		float maxWalk = 12;
		float mult = 6;
		if(Keybindings.sneak.isDown){
			maxWalk *= 0.25F;
		}
		if(!player.isOnGround){
			mult *= 0.25F;
		}
		if(Keybindings.jump.isDown && player.isOnGround){
			player.motionY += 14;
			mult *= 1.2F;
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
			player.addVelocity(player.rightVec.scale(amount*mult*0.75F));
		}
		if(Keybindings.left.isDown){
			float amount = MathHelper.clamp(maxWalk-player.getVelocity().dot(player.leftVec), 0, 1);
			player.addVelocity(player.leftVec.scale(amount*mult*0.75F));
		}
	}
	
	public static enum ConnectionState {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING;
	}
}
