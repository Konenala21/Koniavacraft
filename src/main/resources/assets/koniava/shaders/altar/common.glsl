#version 150

uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;

// Up to 5 ring waves: x=radius (blocks), y=alpha, z=thickness (blocks); zero = inactive
uniform vec3  Waves[5];

// Pillar
uniform float PillarAlpha;
uniform float PillarRadius;

in  vec2 texCoord;
out vec4 fragColor;

const float PI     = 3.14159265;
const float RING_Y = -1.8;   // ring plane offset below altar block top

const vec3 C_CORE   = vec3(0.14, 0.48, 1.00);
const vec3 C_BRIGHT = vec3(0.60, 0.86, 1.00);
const vec3 C_WAVE   = vec3(0.88, 0.95, 1.00);

vec3 worldPos(vec3 uvd) {
    vec3 ndc = uvd * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}
