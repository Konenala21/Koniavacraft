#version 430 core
in vec2 Position;
out vec2 texCoord;
void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    texCoord = Position * 0.5 + 0.5;
}
