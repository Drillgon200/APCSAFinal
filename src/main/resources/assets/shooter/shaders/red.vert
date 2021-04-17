#version 330 compatibility

layout (location = 0) in vec3 position;

uniform mat4 view_matrix;

void main(){
	gl_Position = gl_ProjectionMatrix * view_matrix * gl_ModelViewMatrix * vec4(position, 1);
}