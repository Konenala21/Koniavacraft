#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Saturation;

out vec4 fragColor;

void main(){
    vec4 color = texture(DiffuseSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 result = mix(vec3(luma), color.rgb, Saturation);
    fragColor = vec4(result, color.a);
}
