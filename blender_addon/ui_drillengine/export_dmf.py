import bpy
import bmesh
import struct
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

def processSkeleton(bones):
	skele_node = DocumentNode("skeleton")
	names = ByteBuf()
	parent_ids = ByteBuf()
	bind_transforms = ByteBuf()
	for bone in bones:
		name = bone.name
		if(bone.parent):
			bind_pose = bone.parent.matrix_local.inverted() @ bone.matrix_local
		else:
			bind_pose = bone.matrix_local.copy()
		bind_pose = rotate @ bind_pose
		
		if(bone.parent is None):
			parent = -1
		else:
			parent = bones.items().index((bone.parent.name, bone.parent))
			
		names.writeString(name)
		parent_ids.writeInt(parent)
		bind_transforms.writeMatrix(bind_pose)
	
	skele_node.data.append(DocumentData("names", names))
	skele_node.data.append(DocumentData("parent_ids", parent_ids))
	skele_node.data.append(DocumentData("bind_transforms", bind_transforms))
	
	return skele_node

def processGeo(mat_names, materials, obj):
	if(obj.data.materials[0].name not in mat_names):
		mat_names.append(obj.data.materials[0].name)
		materials.append(obj.data.materials[0])
	
	geometry = DocumentNode(obj.name)
	geometry.data.append(DocumentData("material_id", ByteBuf().writeInt(mat_names.index(obj.data.materials[0].name))))
	me = obj.to_mesh().copy()
	mesh_triangulate(me)
	me.transform(rotate)
	me.calc_normals_split()
	me.calc_tangents()
	
	positions = []
	tex_coords = []
	normals = []
	tangents = []
	faces = []
	weights = []
	
	#Hashing makes things go speed
	pos_dict = {}
	uv_dict = {}
	norm_dict = {}
	tan_dict = {}
	skin_dict = {}

	for poly in me.polygons:
		for vert, loop_idx in zip(poly.vertices, poly.loop_indices):
			loop = me.loops[loop_idx]
			bpos = me.vertices[vert].co
			pos = (bpos.x, bpos.y, bpos.z)
			buv = me.uv_layers.active.data[loop_idx].uv
			uv = (buv.x, buv.y)
			bnormal = loop.normal
			normal = (bnormal.x, bnormal.y, bnormal.z)
			btangent = loop.tangent
			tangent = (btangent.x, btangent.y, btangent.z)
			weight = getBoneIndicesAndWeights(me.vertices[vert], obj, obj.parent.data.bones)
			
			if pos not in pos_dict:
				idx = len(positions)
				pos_dict[pos] = idx
				positions.append(pos)
			if uv not in uv_dict:
				idx = len(tex_coords)
				uv_dict[uv] = idx
				tex_coords.append(uv)
			if normal not in norm_dict:
				idx = len(normals)
				norm_dict[normal] = idx
				normals.append(normal)
			if tangent not in tan_dict:
				idx = len(tangents)
				tan_dict[tangent] = idx
				tangents.append(tangent)
			if weight not in skin_dict:
				idx = len(weights)
				skin_dict[weight] = idx
				weights.append(weight)
			faces.append(pos_dict[pos])
			faces.append(uv_dict[uv])
			faces.append(norm_dict[normal])
			faces.append(tan_dict[tangent])
			faces.append(skin_dict[weight])

	buf = ByteBuf()
	for pos in positions:
		buf.writeFloat(pos[0])
		buf.writeFloat(pos[1])
		buf.writeFloat(pos[2])
	geometry.data.append(DocumentData("positions", buf))
	
	buf = ByteBuf()
	for tex in tex_coords:
		buf.writeFloat(tex[0])
		buf.writeFloat(tex[1])
	geometry.data.append(DocumentData("tex_coords", buf))
	
	buf = ByteBuf()
	for norm in normals:
		buf.writeFloat(norm[0])
		buf.writeFloat(norm[1])
		buf.writeFloat(norm[2])
	geometry.data.append(DocumentData("normals", buf))
	
	buf = ByteBuf()
	for tan in tangents:
		buf.writeFloat(tan[0])
		buf.writeFloat(tan[1])
		buf.writeFloat(tan[2])
	geometry.data.append(DocumentData("tangents", buf))
	
	buf = ByteBuf()
	for weight in weights:
		for pair in weight:
			buf.writeInt(pair[0])
			buf.writeFloat(pair[1])
	geometry.data.append(DocumentData("skinning_data", buf))
	
	buf = ByteBuf()
	for idx in faces:
		buf.writeInt(idx)
	geometry.data.append(DocumentData("faces", buf))
	
	return geometry

def weightComparator(pair):
	return pair[1]

# Returns a list of 4 pairs representing bone indices and weights
def getBoneIndicesAndWeights(vertex, obj, bones):
	pairs = []
	for group in vertex.groups:
		name = obj.vertex_groups[group.group].name;
		if(name in bones):
			pairs.append([bones.find(name), group.weight])
	
	#Make sure there are only 4, and those 4 are the biggest ones
	while(len(pairs) < 4):
		pairs.append([0, 0])
	pairs.sort(key=weightComparator, reverse=True)
	while(len(pairs) > 4):
		pairs.pop()
		
	#Normalize in case there were more than 4
	sum = 0
	for pair in pairs:
		sum += pair[1]
	sum = 1/(sum*0.25)
	for pair in pairs:
		pair[1] *= sum
		
	tuplePairs = ((pairs[0][0], pairs[0][1]), (pairs[1][0], pairs[1][1]), (pairs[2][0], pairs[2][1]), (pairs[3][0], pairs[3][1]))
	
	return tuplePairs

def writeMaterials(materials):
	materials_node = DocumentNode("materials")
	
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
		
		materials_node.data.append(DocumentData(mat.name, mat_buf))
	return materials_node
		

def save(path):
	bpy.context.scene.frame_set(bpy.context.scene.frame_start)

	out_file = open(path, 'wb')
	doc = Document()
	geometry = DocumentNode("geometry")
	
	mat_names = []
	materials = []
	
	obj = bpy.context.active_object
	
	if(obj is not None and obj.type == 'ARMATURE' and obj.parent == None):
		t = time.time()
		doc.children.append(processSkeleton(obj.data.bones))
		for child in getChildren(obj):
			geometry.children.append(processGeo(mat_names, materials, child))
		doc.children.append(geometry)

	doc.children.append(writeMaterials(materials))
	
	out_file.write(doc.toBuffer())
	out_file.close()
	return {'FINISHED'}