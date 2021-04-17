import bpy
import bmesh
import struct
import sys
from .utility import *
from bpy_extras.io_utils import axis_conversion

rotate = axis_conversion(from_forward='-Y', 
        from_up='Z',
        to_forward='Z',
        to_up='Y').to_4x4()
	
def mesh_triangulate(me):
	import bmesh
	bm = bmesh.new()
	bm.from_mesh(me)
	bmesh.ops.triangulate(bm, faces=bm.faces)
	bm.to_mesh(me)
	bm.free()

def getBoundingBox(mesh):
	vals = [sys.float_info.max, sys.float_info.max, sys.float_info.max, -sys.float_info.max, -sys.float_info.max, -sys.float_info.max];
	for poly in mesh.polygons:
		for vert, loop_idx in zip(poly.vertices, poly.loop_indices):
			pos = mesh.vertices[vert].co
			vals[0] = min(vals[0], pos.x);
			vals[1] = min(vals[1], pos.y);
			vals[2] = min(vals[2], pos.z);
			vals[3] = max(vals[3], pos.x);
			vals[4] = max(vals[4], pos.y);
			vals[5] = max(vals[5], pos.z);
	return vals

def processLevelGeo(mat_names, materials, buf, obj):
	if(obj.data.materials[0].name not in mat_names):
		mat_names.append(obj.data.materials[0].name)
		materials.append(obj.data.materials[0])

	buffer = ByteBuf()
	buffer.writeString("material_id")
	buffer.writeInt(4)
	buffer.writeInt(mat_names.index(obj.data.materials[0].name))
	me = obj.to_mesh().copy()
	mesh_triangulate(me)
	me.transform(obj.matrix_world)
	me.transform(rotate)
	me.calc_normals_split()
	me.calc_tangents()
	buffer.writeString("box")
	buffer.writeInt(24)
	box = getBoundingBox(me);
	buffer.writeFloat(box[0])
	buffer.writeFloat(box[1])
	buffer.writeFloat(box[2])
	buffer.writeFloat(box[3])
	buffer.writeFloat(box[4])
	buffer.writeFloat(box[5])
	positions = []
	tex_coords = []
	lmap_tex_coords = []
	normals = []
	tangents = []
	faces = []
	for poly in me.polygons:
		for vert, loop_idx in zip(poly.vertices, poly.loop_indices):
			loop = me.loops[loop_idx]
			pos = me.vertices[vert].co
			uv = me.uv_layers[0].data[loop_idx].uv
			lmap_uv = me.uv_layers['lmap'].data[loop_idx].uv
			normal = loop.normal
			tangent = loop.tangent
			if pos not in positions:
				positions.append(pos)
			if uv not in tex_coords:
				tex_coords.append(uv)
			if lmap_uv not in lmap_tex_coords:
				lmap_tex_coords.append(lmap_uv)
			if normal not in normals:
				normals.append(normal)
			if tangent not in tangents:
				tangents.append(tangent)
			faces.append(positions.index(pos))
			faces.append(tex_coords.index(uv))
			faces.append(lmap_tex_coords.index(lmap_uv))
			faces.append(normals.index(normal))
			faces.append(tangents.index(tangent))

	
	data = ByteBuf()
	for pos in positions:
		data.writeFloat(pos.x)
		data.writeFloat(pos.y)
		data.writeFloat(pos.z)
	buffer.writeString("positions")
	buffer.writeInt(len(data))
	buffer.extend(data)
	
	data = ByteBuf()
	for tex in tex_coords:
		data.writeFloat(tex.x)
		data.writeFloat(tex.y)
	buffer.writeString("tex_coords")
	buffer.writeInt(len(data))
	buffer.extend(data)
	
	data = ByteBuf()
	for tex in lmap_tex_coords:
		data.writeFloat(tex.x)
		data.writeFloat(tex.y)
	buffer.writeString("lmap_tex_coords")
	buffer.writeInt(len(data))
	buffer.extend(data)
	
	data = ByteBuf()
	for norm in normals:
		data.writeFloat(norm.x)
		data.writeFloat(norm.y)
		data.writeFloat(norm.z)
	buffer.writeString("normals")
	buffer.writeInt(len(data))
	buffer.extend(data)
	
	data = ByteBuf()
	for tan in tangents:
		data.writeFloat(tan.x)
		data.writeFloat(tan.y)
		data.writeFloat(tan.z)
	buffer.writeString("tangents")
	buffer.writeInt(len(data))
	buffer.extend(data)
	
	data = ByteBuf()
	for idx in faces:
		data.writeInt(idx)
	buffer.writeString("faces")
	buffer.writeInt(len(data))
	buffer.extend(data)
			
	buf.writeString(obj.name)
	buf.writeInt(len(buffer))
	buf.writeInt(len(buffer))
	buf.extend(buffer)

def processLevelCollision(obj, positions, indices):
	# MUST copy the mesh, otherwise the data can still get updated by other code, causing annoying to track down bugs
	me = obj.to_mesh().copy()
	mesh_triangulate(me)
	me.transform(obj.matrix_world)
	me.transform(rotate)
	for poly in me.polygons:
		for vert, loop_idx in zip(poly.vertices, poly.loop_indices):
			pos = me.vertices[vert].co
			if pos not in positions:
				positions.append(pos)
			indices.append(positions.index(pos))

def writeLevelCollision(out_file, positions, indices):
	buf = ByteBuf()
	pos_bytes = len(positions)*3*4;
	ind_bytes = len(indices)*4;
	buf.writeString("positions")
	buf.writeInt(pos_bytes)
	for pos in positions:
		buf.writeFloat(pos.x)
		buf.writeFloat(pos.y)
		buf.writeFloat(pos.z)
	buf.writeString("indices")
	buf.writeInt(ind_bytes)
	for i in indices:
		buf.writeInt(i)
		
	out_file.write(struct.pack('>i', 17))
	out_file.write("terrain_collision".encode('ascii'))
	out_file.write(struct.pack('>i', len(buf)))
	out_file.write(struct.pack('>i', len(buf)))
	out_file.write(buf)

def writeMaterials(out_file, materials):
	buffer = ByteBuf()
	for mat in materials:
		tree = mat.node_tree
		bsdf = tree.nodes['Principled BSDF']
		color = bsdf.inputs['Base Color'].links[0].from_node.image.name
		metallic = bsdf.inputs['Metallic'].links[0].from_node.image.name
		normal = tree.nodes['Normal Map'].inputs['Color'].links[0].from_node.image.name
		specular = bsdf.inputs['Specular'].default_value
		roughness = bsdf.inputs['Roughness'].default_value
		ior = bsdf.inputs['IOR'].default_value
		emission = bsdf.inputs['Emission'].default_value
		
		mat_buf = ByteBuf()
		mat_buf.writeString(color)
		mat_buf.writeString(metallic)
		mat_buf.writeString(normal)
		mat_buf.writeFloat(specular)
		mat_buf.writeFloat(roughness)
		mat_buf.writeFloat(ior)
		mat_buf.writeFloat(emission[0])
		mat_buf.writeFloat(emission[1])
		mat_buf.writeFloat(emission[2])
		mat_buf.writeFloat(emission[3])
		
		buffer.writeString(mat.name)
		buffer.writeInt(len(mat_buf))
		buffer.extend(mat_buf)
	
	out_file.write(struct.pack('>i', 9))
	out_file.write("materials".encode('ascii'))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(buffer)

def writeSpawns(out_file, positions):
	buffer = ByteBuf()
	buf = ByteBuf()
	for pos in positions:
		buf.writeFloat(pos.x)
		buf.writeFloat(pos.y)
		buf.writeFloat(pos.z)
		
	buffer.writeString("positions")
	buffer.writeInt(len(buf))
	buffer.extend(buf)
		
	out_file.write(struct.pack('>i', 6))
	out_file.write("spawns".encode('ascii'))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(buffer)

def writeLights(out_file, lights):
	buffer = ByteBuf()
	for light in lights:
		if(not (light.data.type == 'POINT' or light.data.type == 'SPOT' or light.data.type == 'SUN')):
			continue
		buf = ByteBuf()
		mat = rotate @ light.matrix_world
		pos = rotate @ light.location
		color = light.data.color
		energy = light.data.energy
		
		buf.writeString(light.data.type)
		buf.writeFloat(color.r)
		buf.writeFloat(color.g)
		buf.writeFloat(color.b)
		buf.writeFloat(energy)
		if(light.data.type == 'SUN'):
			buf.writeFloat(-mat[0][2])
			buf.writeFloat(-mat[1][2])
			buf.writeFloat(-mat[2][2])
		elif(light.data.type == 'SPOT'):
			buf.writeFloat(pos.x)
			buf.writeFloat(pos.y)
			buf.writeFloat(pos.z)
			buf.writeFloat(-mat[0][2])
			buf.writeFloat(-mat[1][2])
			buf.writeFloat(-mat[2][2])
			buf.writeFloat(light.data.spot_size)
			buf.writeFloat(light.data.spot_blend)
		elif(light.data.type == 'POINT'):
			buf.writeFloat(pos.x)
			buf.writeFloat(pos.y)
			buf.writeFloat(pos.z)
		
		buffer.writeString(light.name)
		buffer.writeInt(len(buf))
		buffer.extend(buf)
		
	out_file.write(struct.pack('>i', 6))
	out_file.write("lights".encode('ascii'))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(struct.pack('>i', len(buffer)))
	out_file.write(buffer)

def save(path):
	bpy.context.scene.frame_set(0)

	out_file = open(path, 'wb')
	out_file.write("DUCK".encode('ascii'))
	
	level_geo = ByteBuf();
	mat_names = []
	materials = []
	
	lights = []
	
	positions = []
	indices = []
	spawns = []

	for obj2 in bpy.context.selected_objects:
		if(not obj2.dignore):
			if(obj2.type == 'MESH'):
				if(obj2.dterraincollider):
					processLevelCollision(obj2, positions, indices)
				if(obj2.dlgeo):
					processLevelGeo(mat_names, materials, level_geo, obj2)
			if(obj2.dspawn):
				spawns.append(rotate @ obj2.location)
			if(obj2.type == 'LIGHT'):
				lights.append(obj2)
					
	writeMaterials(out_file, materials)
	writeLights(out_file, lights)
	writeSpawns(out_file, spawns)
	
	data = ByteBuf()
	data.writeString("level_geo")
	data.writeInt(len(level_geo))
	data.writeInt(0)
	out_file.write(data)
	out_file.write(level_geo)
	
	writeLevelCollision(out_file, positions, indices)
	out_file.write("EOF".encode('ascii'))
	out_file.close()
	return {'FINISHED'}