#version 330 compatibility

uniform sampler2D texture;
uniform vec4 color;

in vec2 pass_texCoord;

void main(){
	gl_FragColor = color * vec4(1, 1, 1, texture2D(texture, pass_texCoord).r);
}