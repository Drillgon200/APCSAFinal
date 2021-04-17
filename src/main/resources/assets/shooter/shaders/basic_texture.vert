#version 330 compatibility

layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 tex;

uniform mat4 view_matrix;

out vec2 texCoord;

void main(){
	texCoord = tex;
	gl_Position = gl_ProjectionMatrix * view_matrix * gl_ModelViewMatrix * vec4(pos, 1);
}