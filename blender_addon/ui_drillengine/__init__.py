bl_info = {
	"name": "Drillgon Engine Helper",
	"author": "Drillgon200",
	"version": (1, 0, 0),
	"blender": (2, 90, 1),
	"description": "Helps with creating levels for drill engine games",
	"warning": "",
	"category": "User-Interface",
}

if "bpy" in locals():
	import importlib
	if "export_daf" in locals():
		importlib.reload(export_daf)
	if "export_dmf" in locals():
		importlib.reload(export_dmf)
	if "export_dlf" in locals():
		importlib.reload(export_dlf)

import bpy
from math import *

from . import export_daf
from . import export_dmf
from . import export_dlf

from bpy.props import (
		BoolProperty,
		FloatProperty,
		StringProperty,
		EnumProperty,
		PointerProperty,
		)
from bpy_extras.io_utils import (
		ImportHelper,
		ExportHelper,
		orientation_helper,
		path_reference_mode,
		axis_conversion,
		)
from bpy.types import (Panel,
					   Operator,
					   PropertyGroup,
					   )

def kine_changed(self, context):
	ob = context.object
		
bpy.types.Object.dkinematic = bpy.props.BoolProperty(name = "Kinematic", default = False, update = kine_changed)
bpy.types.Object.dspawn = bpy.props.BoolProperty(name = "Player Spawn", default = False)
bpy.types.Object.dignore = bpy.props.BoolProperty(name = "Ignore Object", default = False)
bpy.types.Object.dterraincollider = bpy.props.BoolProperty(name = "Terrain Collision", default = False)
bpy.types.Object.dlgeo = bpy.props.BoolProperty(name = "Level Geometry", default = False)

#Drill Animation format
@orientation_helper(axis_forward='-Z', axis_up='Y')
class ExportDAF(bpy.types.Operator, ExportHelper):
	bl_idname = "export_scene.daf"
	bl_label = 'Export DAF'
	bl_options = {'PRESET'}

	filename_ext = ".daf"
	filter_glob: StringProperty(
			default="*.daf",
			options={'HIDDEN'},
			)

	global_scale: FloatProperty(
			name="Scale",
			min=0.01, max=1000.0,
			default=1.0,
			)

	path_mode: path_reference_mode

	check_extension = True

	def execute(self, context):
		return export_daf.save(self.filepath)
	def draw(self, context):
		layout = self.layout
		sfile = context.space_data
		operator = sfile.active_operator
		layout.prop(operator, 'global_scale')

#Drill Model Format
@orientation_helper(axis_forward='-Z', axis_up='Y')
class ExportDMF(bpy.types.Operator, ExportHelper):
	bl_idname = "export_scene.dmf"
	bl_label = 'Export DMF'
	bl_options = {'PRESET'}

	filename_ext = ".dmf"
	filter_glob: StringProperty(
			default="*.dmf",
			options={'HIDDEN'},
			)

	global_scale: FloatProperty(
			name="Scale",
			min=0.01, max=1000.0,
			default=1.0,
			)

	path_mode: path_reference_mode

	check_extension = True

	def execute(self, context):
		return export_dmf.save(self.filepath)
	def draw(self, context):
		layout = self.layout
		sfile = context.space_data
		operator = sfile.active_operator
		layout.prop(operator, 'global_scale')

#Drill Level Format
@orientation_helper(axis_forward='-Z', axis_up='Y')
class ExportDLF(bpy.types.Operator, ExportHelper):
	bl_idname = "export_scene.dlf"
	bl_label = 'Export DLF'
	bl_options = {'PRESET'}

	filename_ext = ".dlf"
	filter_glob: StringProperty(
			default="*.dlf",
			options={'HIDDEN'},
			)

	global_scale: FloatProperty(
			name="Scale",
			min=0.01, max=1000.0,
			default=1.0,
			)

	path_mode: path_reference_mode

	check_extension = True

	def execute(self, context):
		return export_dlf.save(self.filepath)
	def draw(self, context):
		layout = self.layout
		sfile = context.space_data
		operator = sfile.active_operator
		layout.prop(operator, 'global_scale')

class DrillPanel(bpy.types.Panel):
	bl_label = "Drill Engine"
	bl_idname = "OBJECT_PT_drill_engine"
	bl_space_type = 'PROPERTIES'
	bl_region_type = 'WINDOW'
	bl_context = "object"
	
	def draw(self, context):
		layout = self.layout
		obj = context.object
		row = layout.row()
		row.label(text = "Properties")
		
		row = layout.row()
		row.prop(obj, "dkinematic")
		row = layout.row()
		row.prop(obj, "dspawn")
		row = layout.row()
		row.prop(obj, "dignore")
		row = layout.row()
		row.prop(obj, "dterraincollider")
		row = layout.row()
		row.prop(obj, "dlgeo")

class CopyLightNodesOperator(bpy.types.Operator):
	bl_idname = "drill.copy_light_nodes"
	bl_label = "Copy light nodes from selected object"
	
	def copy_links(self, treeFrom, treeTo):
		for node in treeFrom.nodes:
			new_node = treeTo.nodes[node.name]
			for i, inp in enumerate(node.inputs):
				for link in inp.links:
					from_node = treeTo.nodes[link.from_node.name]
					treeTo.links.new(from_node.outputs[link.from_socket.name], new_node.inputs[i])
		
	def copy_attr(self, id, nodeFrom, nodeTo):
		if(hasattr(nodeTo, id)):
			setattr(nodeTo, id, getattr(nodeFrom, id))
	
	def copy_attributes(self, nodeFrom, nodeTo):

		#all attributes that shouldn't be copied
		ignore_attributes = ( "rna_type", "type", "dimensions", "inputs", "outputs", "internal_links", "select", "interface")

		for attr in nodeFrom.bl_rna.properties:
			#check if the attribute should be copied and add it to the list of attributes to copy
			id = attr.identifier
			if not id in ignore_attributes and not id.split("_")[0] == "bl":
				self.copy_attr(id, nodeFrom, nodeTo)
	
	def copy_nodes(self, treeFrom, treeTo):
		
		#the attributes that should be copied for every link
		input_attributes = ( "default_value", "name" )
		output_attributes = ( "default_value", "name" )
		
		for node in treeFrom.nodes:
			new_node = treeTo.nodes.new(node.bl_idname)
			new_node.location = node.location.copy()
			self.copy_attributes(node, new_node)
			for i, inp in enumerate(node.inputs):
				for id in input_attributes:
					self.copy_attr(id, inp, new_node.inputs[i])
			for i, out in enumerate(node.outputs):
				for id in output_attributes:
					self.copy_attr(id, out, new_node.outputs[i])

		self.copy_links(treeFrom, treeTo)

	
	def execute(self, context):
		lights = {obj for obj in bpy.context.selected_objects if (obj.type == 'LIGHT' and not obj.dignore)}
		
		new_nodes = bpy.context.active_object.data.node_tree
		

		for light in lights:
			if(light != bpy.context.active_object):
				light.data.use_nodes = True
				light.data.node_tree.nodes.clear()
				self.copy_nodes(new_nodes, light.data.node_tree)
				light.data.node_tree.interface_update(bpy.context)
			for node in light.data.node_tree.nodes:
				if(node.label == 'light_radius'):
					node.outputs[0].default_value = sqrt(sqrt(light.data.energy)/0.15-1)
		return {'FINISHED'}
		
class SetLightColorsOperator(bpy.types.Operator):
	bl_idname = "drill.set_light_colors"
	bl_label = "Sets all selected light colors to the active color"
	
	def execute(self, context):
		lights = {obj for obj in bpy.context.selected_objects if (obj.type == 'LIGHT' and not obj.dignore)}
		
		new_color = bpy.context.active_object.data.color
		
		for light in lights:
			if(light != bpy.context.active_object):
				light.data.color = new_color.copy()
		return {'FINISHED'}

class LmapEnableOperator(bpy.types.Operator):
	bl_idname = "drill.create_lmap"
	bl_label = "Create and Enable Lightmap UV Maps"
	
	def execute(self, context):
		if('Lightmap' not in bpy.data.images):
			bpy.ops.image.new(name='Lightmap', width=1024, height=1024, color=(0, 0, 0, 0), alpha=True, generated_type='BLANK', float=True, use_stereo_3d=False, tiled=False)
			print("Lightmap not found, creating new")
		img = bpy.data.images['Lightmap']
		
	
		meshes = {obj for obj in bpy.context.selected_objects if (obj.type == 'MESH' and not obj.dignore)}

		for mesh in meshes:
			if('lmap' not in mesh.data.uv_layers):
				mesh.data.uv_layers.new(name='lmap')
			mesh.data.uv_layers['lmap'].active = True
			mat = mesh.material_slots[0].material
			mat.use_nodes = True
			nodes = mat.node_tree.nodes
			if('Bake_node' not in nodes):
				n = nodes.new('ShaderNodeTexImage')
				n.name = 'Bake_node'
			tex_node = nodes['Bake_node']
			tex_node.select = True
			nodes.active = tex_node
			tex_node.image = img
		return {'FINISHED'}

class LmapDisableOperator(bpy.types.Operator):
	bl_idname = "drill.disable_lmap"
	bl_label = "Revert to regular uv maps"
	
	def execute(self, context):
		meshes = {obj.data for obj in bpy.context.selected_objects if obj.type == 'MESH'}

		for mesh in meshes:
			mesh.uv_layers[0].active = True
		return {'FINISHED'}
		
class DrillPanelGlobal(bpy.types.Panel):
	bl_label = "Drill Engine"
	bl_idname = "WORLD_PT_drill_engine"
	bl_space_type = 'PROPERTIES'
	bl_region_type = 'WINDOW'
	bl_context = "world"
	
	def draw(self, context):
		layout = self.layout
		row = layout.row()
		row.label(text = "Tools")
		
		row = layout.row()
		row.operator("drill.copy_light_nodes", text="Copy light nodes", text_ctxt="", translate=True, icon='NONE', emboss=True, depress=False, icon_value=0)
		row = layout.row()
		row.operator("drill.set_light_colors", text="Set light colors", text_ctxt="", translate=True, icon='NONE', emboss=True, depress=False, icon_value=0)
		row = layout.row()
		row.operator("drill.create_lmap", text="Activate lightmap UV layers", text_ctxt="", translate=True, icon='NONE', emboss=True, depress=False, icon_value=0)
		row = layout.row()
		row.operator("drill.disable_lmap", text="Deactive lightmap", text_ctxt="", translate=True, icon='NONE', emboss=True, depress=False, icon_value=0)

def menu_func_export(self, context):
	self.layout.operator(ExportDAF.bl_idname, text="Drill Animation Format")
	self.layout.operator(ExportDMF.bl_idname, text="Drill Model Format")
	self.layout.operator(ExportDLF.bl_idname, text="Drill Level Format")

classes = (
	DrillPanel,
	CopyLightNodesOperator,
	SetLightColorsOperator,
	LmapEnableOperator,
	LmapDisableOperator,
	DrillPanelGlobal,
	ExportDAF,
	ExportDMF,
	ExportDLF,
)

def register():
	from bpy.utils import register_class
	for cls in classes:
		register_class(cls)
	bpy.types.TOPBAR_MT_file_export.append(menu_func_export)

def unregister():
	from bpy.utils import unregister_class
	for cls in reversed(classes):
		unregister_class(cls)
	bpy.types.TOPBAR_MT_file_export.remove(menu_func_export)
	

if __name__ == "__main__":
	register()