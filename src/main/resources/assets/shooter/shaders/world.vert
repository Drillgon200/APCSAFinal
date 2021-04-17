#version 330 compatibility

const int MAX_LIGHTS = 200;
const int MAX_ACTIVE_LIGHTS = 20;

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 tex;
layout (location = 2) in vec3 norm;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec2 lmapTex;

struct PointLight {
	//Use a single vec4 for color and strength to save space
	vec4 colorAndStrength;
	//Might put something in the w component later, I don't know
	vec4 pos;
};


//uniform PointLight lights[MAX_LIGHTS];

//layout (std140) uniform PointLights {
//	PointLight pointLights[MAX_LIGHTS];
//} lights;
//uniform int lightIndices[MAX_ACTIVE_LIGHTS];
//uniform int lightCount;
uniform vec3 view_pos;
uniform mat4 view_matrix;

//out vec3 pointLightPositions[MAX_ACTIVE_LIGHTS];
out vec3 camPos;
out vec3 worldPos;
out vec2 texCoord;
out vec2 lmapTexCoord;
out vec3 worldNormal;
out vec3 worldTangent;

mat3 generateTBN(vec3 N, vec3 T){
	vec3 B = cross(N, T);
	return transpose(mat3(T, B, N));
}

void main(){
	texCoord = tex;
	lmapTexCoord = lmapTex;
	worldNormal = gl_NormalMatrix * norm;
	worldTangent = gl_NormalMatrix * tangent;
	//mat3 TBN = generateTBN(worldNormal, worldTangent);
	//for(int i = 0; i < lightCount; i ++){
	//	int idx = lightIndices[i];
	//	pointLightPositions[i] = TBN * vec3(gl_ModelViewMatrix * vec4(lights.pointLights[idx].pos.xyz, 1));
	//}
	camPos = vec3(gl_ModelViewMatrix * vec4(view_pos, 1));
	
	worldPos = vec3(gl_ModelViewMatrix * vec4(position, 1));
	gl_Position = gl_ProjectionMatrix * view_matrix * gl_ModelViewMatrix * vec4(position, 1);
}