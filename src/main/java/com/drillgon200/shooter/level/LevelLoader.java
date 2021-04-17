package com.drillgon200.shooter.level;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.Pair;
import com.drillgon200.physics.TriangleCollider;
import com.drillgon200.shooter.fileformat.DataUtil;
import com.drillgon200.shooter.fileformat.DocumentLoader;
import com.drillgon200.shooter.fileformat.DocumentLoader.DocumentData;
import com.drillgon200.shooter.fileformat.DocumentLoader.DocumentNode;
import com.drillgon200.shooter.render.Light;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.render.StaticGeometry;
import com.drillgon200.shooter.render.Vertex;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Triangle;
import com.drillgon200.shooter.util.Vec3f;

public class LevelLoader {

	@SuppressWarnings("unchecked")
	private static void loadClient(String path, DocumentNode doc, LevelClient level){
		List<Material> materials = new ArrayList<>();
		for(DocumentData mat : doc.getChild("materials").data) {
			materials.add(DataUtil.readMaterial(mat));
		}
		level.levelMaterials = materials.toArray(new Material[materials.size()]);
		level.renderersByMaterial = new Queue[materials.size()];
		for(int i = 0; i < level.renderersByMaterial.length; i++) {
			//ArrayDeques are faster than ArrayLists apparently, and the API part I need is exactly the same, so I might as well use them.
			level.renderersByMaterial[i] = new ArrayDeque<>();
		}
		
		level.geometryVao = generateLevelGeometry(level, doc);

		for(DocumentData lightData : doc.getChild("lights").data) {
			ByteBuffer buf = ByteBuffer.wrap(lightData.data);
			String type = DocumentLoader.readString(buf);
			Vec3f color = new Vec3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
			float energy = (float) Math.sqrt(buf.getFloat());
			Light light;
			if(type.equals("POINT")) {
				Vec3f pos = new Vec3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
				light = new Light().pointlight(pos, color, energy);
			} else if(type.equals("SPOT")) {
				Vec3f pos = new Vec3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
				Vec3f dir = new Vec3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
				float size = buf.getFloat();
				size = (float) Math.acos(size);
				float blend = buf.getFloat();
				//Possibly use blend squared here? Blender's blend value looks not linear to me. Maybe they use some cosine trickery
				float innerAngle = size * (1 - blend);
				light = new Light().spotlight(pos, dir, color, energy, innerAngle, size);
			} else if(type.equals("SUN")) {
				Vec3f dir = new Vec3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
				light = new Light().sunlight(dir, color, energy);
			} else {
				throw new RuntimeException("Invalid light type!");
			}
			level.staticLights.insert(light);
		}

		level.lightmaps = new String[2];
		level.lightmaps[0] = "/assets/shooter/textures/level/" + level.name + "/lmap_color.png";
		level.lightmaps[1] = "/assets/shooter/textures/level/" + level.name + "/lmap_direction.png";
		TextureManager.loadTexture(level.lightmaps[0]);
		TextureManager.filterMipmap(level.lightmaps[0], true, true);
		TextureManager.loadTexture(level.lightmaps[1]);
		TextureManager.filterMipmap(level.lightmaps[1], true, true);
	}

	private static void loadServer(String path, DocumentNode doc, LevelCommon level){
		String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
		level.name = name;

		addTerrainCollision(level, doc.getChild("terrain_collision"));

		float[] spawns = doc.getChild("spawns").getData("positions").getFloatArray();
		for(int i = 0; i < spawns.length; i += 3) {
			level.possibleSpawns.add(new Vec3f(spawns[i], spawns[i + 1], spawns[i + 2]));
		}
	}
	
	public static LevelClient loadClient(String path){
		DocumentNode doc = DocumentLoader.parseDocument(path);
		LevelClient level = new LevelClient();
		loadServer(path, doc, level);
		loadClient(path, doc, level);
		
		return level;
	}
	
	public static LevelCommon loadServer(String path) {
		DocumentNode doc = DocumentLoader.parseDocument(path);
		LevelCommon level = new LevelCommon();
		loadServer(path, doc, level);

		return level;
	}

	private static int generateLevelGeometry(LevelClient level, DocumentNode doc) {
		List<Vertex> vertexList = new ArrayList<>();
		for(DocumentNode geodata : doc.getChild("level_geo").children) {
			Pair<StaticGeometry, Vertex[]> geo = parseGeometry(geodata);
			int[] offsetAndCount = new int[]{vertexList.size(), 0};
			for(Vertex vert : geo.right)
				vertexList.add(vert);
			offsetAndCount[1] = vertexList.size() - offsetAndCount[0];
			geo.left.offsetAndCount = offsetAndCount;
			level.staticGeometry.insert(geo.left);
		}
		
		//TODO I can probably cut down on the data needed to store normals and tangents. 10 10 10 2 format?
		int bytesPerVertex = 3 * 4 + 2 * 4 + 3 * 4 + 3 * 4 + 2 * 4;
		ByteBuffer buf = GLAllocation.createDirectByteBuffer(bytesPerVertex * vertexList.size());
		for(Vertex v : vertexList) {
			buf.putFloat(v.pos.x);
			buf.putFloat(v.pos.y);
			buf.putFloat(v.pos.z);

			buf.putFloat(v.u);
			buf.putFloat(v.v);

			buf.putFloat(v.normal.x);
			buf.putFloat(v.normal.y);
			buf.putFloat(v.normal.z);

			buf.putFloat(v.tangent.x);
			buf.putFloat(v.tangent.y);
			buf.putFloat(v.tangent.z);

			buf.putFloat(v.lmapU);
			buf.putFloat(v.lmapV);
		}
		buf.rewind();

		int vbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
		GLAllocation.freeDirectBuffer(buf);

		int vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);

		//Pos
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, bytesPerVertex, 0);
		GL20.glEnableVertexAttribArray(0);
		//Tex
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, bytesPerVertex, 12);
		GL20.glEnableVertexAttribArray(1);
		//Normal
		GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, bytesPerVertex, 20);
		GL20.glEnableVertexAttribArray(2);
		//Tangent
		GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, bytesPerVertex, 32);
		GL20.glEnableVertexAttribArray(3);
		//Lmap
		GL20.glVertexAttribPointer(4, 2, GL11.GL_FLOAT, false, bytesPerVertex, 44);
		GL20.glEnableVertexAttribArray(4);

		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		return vao;
	}

	private static Pair<StaticGeometry, Vertex[]> parseGeometry(DocumentNode geo) {
		StaticGeometry geometry = new StaticGeometry();
		float[] boxdata = geo.getData("box").getFloatArray();
		geometry.box = new AxisAlignedBB(boxdata[0], boxdata[1], boxdata[2], boxdata[3], boxdata[4], boxdata[5]);
		geometry.materialIndex = geo.getData("material_id").getInt();

		float[] positions = geo.getData("positions").getFloatArray();
		float[] texCoords = geo.getData("tex_coords").getFloatArray();
		float[] lmapTexCoords = geo.getData("lmap_tex_coords").getFloatArray();
		float[] normals = geo.getData("normals").getFloatArray();
		float[] tangents = geo.getData("tangents").getFloatArray();
		int[] faces = geo.getData("faces").getIntArray();

		Vertex[] vertices = new Vertex[faces.length / 5];
		for(int i = 0; i < vertices.length; i++) {
			Vertex v = new Vertex();
			int posIdx = faces[i * 5];
			int texIdx = faces[i * 5 + 1];
			int lmapIdx = faces[i * 5 + 2];
			int normIdx = faces[i * 5 + 3];
			int tanIdx = faces[i * 5 + 4];
			v.pos = new Vec3f(positions[posIdx * 3], positions[posIdx * 3 + 1], positions[posIdx * 3 + 2]);
			v.u = texCoords[texIdx * 2];
			v.v = texCoords[texIdx * 2 + 1];
			v.lmapU = lmapTexCoords[lmapIdx * 2];
			v.lmapV = lmapTexCoords[lmapIdx * 2 + 1];
			v.normal = new Vec3f(normals[normIdx * 3], normals[normIdx * 3 + 1], normals[normIdx * 3 + 2]);
			v.tangent = new Vec3f(tangents[tanIdx * 3], tangents[tanIdx * 3 + 1], tangents[tanIdx * 3 + 2]);
			vertices[i] = v;
		}
		return new Pair<>(geometry, vertices);
	}

	private static void addTerrainCollision(LevelCommon l, DocumentNode collision) {
		int[] indices = collision.getData("indices").getIntArray();
		float[] positions = collision.getData("positions").getFloatArray();
		for(int i = 0; i < indices.length; i += 3) {
			Vec3f pos1 = new Vec3f(positions[indices[i] * 3], positions[indices[i] * 3 + 1], positions[indices[i] * 3 + 2]);
			Vec3f pos2 = new Vec3f(positions[indices[i + 1] * 3], positions[indices[i + 1] * 3 + 1], positions[indices[i + 1] * 3 + 2]);
			Vec3f pos3 = new Vec3f(positions[indices[i + 2] * 3], positions[indices[i + 2] * 3 + 1], positions[indices[i + 2] * 3 + 2]);
			l.staticColliders.insert(new TriangleCollider(new Triangle(pos1, pos2, pos3)));
		}
	}

}
