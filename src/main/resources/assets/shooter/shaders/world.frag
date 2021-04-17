#version 330 compatibility

const int HEIGHT_SLICES = 8;
const int WIDTH_SLICES = 16;
const int DEPTH_SLICES = 24;
const int STATIC_LIGHT_OFFSET = 0;

const int MAX_LIGHTS = 200;
const int MAX_ACTIVE_LIGHTS = 20;
const float PI = 3.14159265359;
const float PI_RCP = 0.31830988618;

//layout (std140) uniform PointLights {
//	PointLight pointLights[MAX_LIGHTS];
//} lights;

uniform vec2 screenSize;
//Depth scale and bias for getting the cluster
uniform float scale;
uniform float bias;

uniform float zNear;
uniform float zFar;

//uniform int lightIndices[MAX_ACTIVE_LIGHTS];
//uniform PointLight lights[MAX_ACTIVE_LIGHTS];
//uniform int lightCount;

uniform bool applyStaticLights;

uniform sampler2D lmap_color;
uniform sampler2D lmap_direction;

//Moving to clustered forward
uniform isamplerBuffer clusters;
uniform usamplerBuffer index_list;
uniform samplerBuffer light_list;

//in vec3 pointLightPositions[MAX_ACTIVE_LIGHTS];

in vec3 camPos;
in vec3 worldPos;
in vec2 texCoord;
in vec2 lmapTexCoord;
in vec3 worldNormal;
in vec3 worldTangent;

const float rcp_10bits = 1.0/1023.0;
const float rcp_8bits = 1.0/255.0;
const float rcp_2bits = 1.0/3.0;

import utilities;

uniform Material mat;

//PBR approach from https://learnopengl.com/PBR/Theory
//learnopengl.com is pretty much the best resource I've seen for learning about open gl.
//Why is it called GGX? Literally no clue. The original paper basically just said "we're calling this GGX" without any clue as to why.
float DistributionGGX(vec3 N, vec3 H, float roughness){
	float a = roughness*roughness;
	float a2 = a*a;
	float NdotH = max(dot(N, H), 0.0);
	float NdotH2 = NdotH*NdotH;
	
	float denom = NdotH2 * (a2 - 1.0) + 1.0;
	denom = PI * denom * denom;
	
	return a2/denom;
}

float GeometrySchlickGGX(float NdotV, float roughness){
	float r = roughness + 1.0;
	float k = r*r*0.125;
	
	float denom = NdotV * (1.0 - k) + k;
	
	return NdotV/denom;
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness){
	float NdotV = max(dot(N, V), 0.0);
	float NdotL = max(dot(N, L), 0.0);
	float ggx1 = GeometrySchlickGGX(NdotV, roughness);
	float ggx2 = GeometrySchlickGGX(NdotL, roughness);
	
	return ggx1*ggx2;
}

vec3 fresnelSchlick(float cosTheta, vec3 F0){
	return F0 + (1.0 - F0) * pow(max(1.0 - cosTheta, 0.0), 5.0);
}

//Material parameters
vec3 albedo;
vec3 normal;
float metallic;
float specular;
float roughness;

//PBR parameters
vec3 N;
vec3 V;
vec3 F0;

vec3 PBR(vec3 radiance, vec3 L, vec3 H){
	//BRDF
	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
	
	//Metals only reflect, so this scales the albedo down when there's more metal
	vec3 kD = vec3(1.0) - F;
	kD *= 1.0 - metallic;
	
	vec3 numerator = NDF*G*F;
	//dot(N, H) using the halfway vector instead of the view is technically incorrect
	//but I get super annoying artifacts otherwise, so I'll go with this.
	float denom = 4.0*max(dot(N, H), 0.0)*max(dot(N, L), 0.0);
	vec3 spec = numerator / max(denom, 0.001);
		
	//Add to final radiance
	float NdotL = max(dot(N, L), 0.0);
	return (kD*albedo*PI_RCP + spec) * radiance * NdotL;
}

//Epic games lighting model falloff
float pointLightFalloff(float radius, float dist){
	float distOverRad = dist/radius;
	float distOverRad2 = distOverRad*distOverRad;
	float distOverRad4 = distOverRad2*distOverRad2;
	
	float falloff = clamp(1-distOverRad4, 0, 1);
	return (falloff * falloff)/(dist*dist + 1);
}

vec3 PBR(PointLight light){
	vec3 lightPos = light.positionAndRadius.xyz;
	vec3 toLightPos = lightPos-worldPos;
	vec3 L = normalize(toLightPos);
	vec3 H = normalize(V + L);
	float dist = length(toLightPos);
	float attenuation = pointLightFalloff(light.positionAndRadius.w, dist);
	vec3 radiance = light.colorAndStrength.rgb*light.colorAndStrength.w*attenuation;
	
	return PBR(radiance, L, H);
}

vec3 PBR(SpotLight light){
	vec3 lightPos = light.positionAndRadius.xyz;
	vec3 toLightPos = lightPos-worldPos;
	vec3 L = normalize(toLightPos);
	vec3 H = normalize(V + L);
	float dist = length(toLightPos);
	float cosCurrentAngle = dot(-L, light.direction.xyz);
	float attenuation = pointLightFalloff(light.positionAndRadius.w, dist);
	float spotFalloff = clamp((cosCurrentAngle-light.angles.y)/(light.angles.x-light.angles.y), 0, 1);
	vec3 radiance = light.colorAndStrength.rgb*light.colorAndStrength.w*attenuation*spotFalloff;
	
	return PBR(radiance, L, H);
}

vec3 PBR(SunLight light){
	vec3 L = -light.direction.xyz;
	vec3 H = normalize(V + L);
	vec3 radiance = light.colorAndStrength.rgb*light.colorAndStrength.w;
	
	return PBR(radiance, L, H);
}

vec3 applyLightFromData(vec4 positionAndRadius, vec4 data){
	vec3 Lo;
	int light_type = getLightType(data.x);
	if(light_type == 0){
		PointLight light = decodePointLight(positionAndRadius, data);
		Lo = PBR(light);
	} else if(light_type == 1){
		SpotLight light = decodeSpotLight(positionAndRadius, data);
		Lo = PBR(light);
	} else if(light_type == 2){
		SunLight light = decodeSunLight(positionAndRadius, data);
		Lo = PBR(light);
	}
	return Lo;
}

mat3 generateTBN(vec3 N, vec3 T){
	vec3 B = cross(N, T);
	return mat3(T, B, N);
}

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

//Some kind of crude inheritance system. This allows another file to 'override' these methods so I don't have to copy and paste this shader
extension_start:

bool doesLighting(){
	return true;
}

vec3 processLinear(vec3 color){
	return color;
}

vec4 finalProcess(vec4 color){
	return color;
}

extension_end:

void main(){
	vec4 color = texture2D(mat.color, texCoord);
	//Linearize color
	albedo = pow(color.rgb, vec3(2.2));
	mat3 TBN = generateTBN(worldNormal, worldTangent);
	normal = TBN * normalize(texture2D(mat.normal, texCoord).xyz*2.0-1.0);
	metallic = texture2D(mat.metallic, texCoord).r;
	specular = mat.specular;
	roughness = mat.roughness;
	roughness = max(1-metallic, 0.2);
	
	N = normal;
	V = normalize(camPos-worldPos);
	F0 = vec3(0.04);
	F0 = mix(F0, albedo, metallic);
	
	vec3 final_color = vec3(0);
	
	if(doesLighting()){
		//Reflectance
		vec3 Lo = vec3(0.0);
		int clusterIdx = getClusterIndex(gl_FragCoord.xy, linearizeDepth(gl_FragCoord.z));
		ivec3 cluster = texelFetch(clusters, clusterIdx).xyz;
		//Static branching, should be fast right?
		if(applyStaticLights){
			for(int i = 0; i < cluster.y; i ++){
				int staticLightIndex = int(texelFetch(index_list, cluster.x+i).x);
				Lo += applyLightFromData(texelFetch(light_list, staticLightIndex*2), texelFetch(light_list, staticLightIndex*2+1));
			}
		} else {
			vec3 radiance = texture2D(lmap_color, lmapTexCoord).rgb*2;
			vec3 L = normalize(texture2D(lmap_direction, lmapTexCoord).xyz*2.0-1.0)*vec3(1, 1, -1);
			vec3 H = normalize(V + L);
			Lo += PBR(radiance, L, H);
		}
		for(int i = 0; i < cluster.z; i ++){
			int dynamicLightIndex = int(texelFetch(index_list, cluster.x+i).y);
			Lo += applyLightFromData(texelFetch(light_list, dynamicLightIndex*2), texelFetch(light_list, dynamicLightIndex*2+1));
		}
		vec3 ambient = vec3(0.03) * albedo;
		final_color = ambient + Lo;
	}
	
	final_color = processLinear(final_color);
	
	final_color = final_color/(final_color + vec3(1.5));
	//Map back to sRGB
	final_color = pow(final_color, vec3(1.0/2.2));
	
	gl_FragColor = finalProcess(vec4(final_color, color.a));
}