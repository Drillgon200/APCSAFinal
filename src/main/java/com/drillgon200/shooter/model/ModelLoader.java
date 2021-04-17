package com.drillgon200.shooter.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.drillgon200.physics.Pair;
import com.drillgon200.shooter.fileformat.DataUtil;
import com.drillgon200.shooter.fileformat.DocumentLoader;
import com.drillgon200.shooter.fileformat.DocumentLoader.DocumentNode;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.render.Vertex;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.Matrix4f;
import com.drillgon200.shooter.util.Vec3f;

public class ModelLoader {

	public static Model load(String path){
		DocumentNode doc = DocumentLoader.parseDocument(path);
		Pair<Integer, List<Pair<String, int[]>>> geo = genGeometry(doc.getChild("geometry"));
		Bone root = genSkeleton(doc.getChild("skeleton"));
		Material mat = DataUtil.readMaterial(doc.getChild("materials").data[0]);
		return new Model(mat, root, geo.left, geo.right);
	}
	
	public static Bone genSkeleton(DocumentNode skele){
		int boneCount = skele.getData("parent_ids").data.length/4;
		
		String[] names = new String[boneCount];
		int[] ids = skele.getData("parent_ids").getIntArray();
		Matrix4f[] transforms = new Matrix4f[boneCount];
		
		FloatBuffer matrixData = ByteBuffer.wrap(skele.getData("bind_transforms").data).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
		for(int i = 0; i < boneCount; i ++){
			transforms[i] = new Matrix4f().load(matrixData);
		}
		
		ByteBuffer nameData = ByteBuffer.wrap(skele.getData("names").data).order(ByteOrder.BIG_ENDIAN);
		for(int i = 0; i < boneCount; i ++){
			names[i] = DocumentLoader.readString(nameData);
		}
		
		BoneData[] boneData = new BoneData[boneCount];
		for(int i = 0; i < boneCount; i ++){
			boneData[i] = new BoneData(i, ids[i], names[i], transforms[i]);
		}
		Bone root = new Bone("master_root", new Matrix4f().identity(), -1, null);
		addChildren(root, boneData);
		root.calcInvBindTransforms(new Matrix4f().identity());
		return root;
	}
	
	public static Bone addChildren(Bone b, BoneData[] data){
		List<Bone> children = new ArrayList<>();
		for(BoneData dat : data){
			if(dat.parentId == b.index){
				Bone bone = new Bone(dat.name, dat.mat, dat.id, null);
				children.add(addChildren(bone, data));
			}
		}
		b.children = children.toArray(new Bone[children.size()]);
		return b;
	}
	
	//Bruh that's a mess of generics
	public static Pair<Integer, List<Pair<String, int[]>>> genGeometry(DocumentNode model){
		List<Vertex> vertices = new ArrayList<>();
		List<Pair<String, int[]>> geometries = new ArrayList<>();
		
		for(DocumentNode geo : model.children){
			int offset = vertices.size()/3;
			addVertices(geo, vertices);
			int count = vertices.size()/3 - offset;
			geometries.add(new Pair<>(geo.name, new int[]{offset, count}));
		}
		
		//TODO I can probably cut down on the data needed to store normals and tangents. 10 10 10 2 format?
		int bytesPerVertex = 3*4 + 2*4 + 3*4 + 3*4 + 4*1 + 4*1;
		//Position, tex, normal, tangent, bone indices, bone weights
		ByteBuffer buf = GLAllocation.createDirectByteBuffer(bytesPerVertex*vertices.size());
		for(Vertex v : vertices){
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
			
			buf.put(v.boneIndices);
			buf.put(v.boneWeights);
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
		//Indices
		GL20.glVertexAttribPointer(5, 4, GL11.GL_UNSIGNED_BYTE, false, bytesPerVertex, 44);
		GL20.glEnableVertexAttribArray(5);
		//Weights
		GL20.glVertexAttribPointer(6, 4, GL11.GL_UNSIGNED_BYTE, true, bytesPerVertex, 48);
		GL20.glEnableVertexAttribArray(6);
		
		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		return new Pair<>(vao, geometries);
	}
	
	private static void addVertices(DocumentNode geo, List<Vertex> verts){
		float[] positions = geo.getData("positions").getFloatArray();
		float[] texCoords = geo.getData("tex_coords").getFloatArray();
		float[] normals = geo.getData("normals").getFloatArray();
		float[] tangents = geo.getData("tangents").getFloatArray();
		final int BYTES_PER_SKIN_DATA = 4*2*4;
		ByteBuffer skinning = ByteBuffer.wrap(geo.getData("skinning_data").data);
		int[] faces = geo.getData("faces").getIntArray();
		
		Vertex[] vertices = new Vertex[faces.length/5];
		for(int i = 0; i < vertices.length; i ++){
			Vertex v = new Vertex();
			int posIdx = faces[i*5];
			int texIdx = faces[i*5+1];
			int normIdx = faces[i*5+2];
			int tanIdx = faces[i*5+3];
			int skinIdx = faces[i*5+4];
			v.pos = new Vec3f(positions[posIdx*3], positions[posIdx*3+1], positions[posIdx*3+2]);
			v.u = texCoords[texIdx*2];
			v.v = texCoords[texIdx*2+1];
			v.normal = new Vec3f(normals[normIdx*3], normals[normIdx*3+1], normals[normIdx*3+2]);
			v.tangent = new Vec3f(tangents[tanIdx*3], tangents[tanIdx*3+1], tangents[tanIdx*3+2]);
			skinning.position(BYTES_PER_SKIN_DATA*skinIdx);
			v.setSkinningData(skinning);
			vertices[i] = v;
		}
		
		for(Vertex vert : vertices){
			verts.add(vert);
		}
	}
	
	private static class BoneData {
		int id;
		int parentId;
		String name;
		Matrix4f mat;
		
		public BoneData(int id, int parentId, String name, Matrix4f mat) {
			super();
			this.id = id;
			this.parentId = parentId;
			this.name = name;
			this.mat = mat;
		}
		
	}
}
