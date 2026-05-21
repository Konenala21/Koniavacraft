#version 150

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out vec4 vertColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertColor = Color;
}
