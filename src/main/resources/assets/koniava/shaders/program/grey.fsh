#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Saturation;
uniform float RedTint;

out vec4 fragColor;

void main(){
    vec4 color = texture(DiffuseSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 result = mix(vec3(luma), color.rgb, Saturation);
    result = mix(result, result * vec3(1.35, 0.55, 0.55), RedTint); // 危險時泛紅警示
    fragColor = vec4(result, color.a);
}
