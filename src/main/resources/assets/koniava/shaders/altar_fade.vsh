#version 150
void main() {
    // Fullscreen triangle using gl_VertexID — no VBO or attribute location needed
    float x = float(gl_VertexID & 1) * 4.0 - 1.0;
    float y = float((gl_VertexID >> 1) & 1) * 4.0 - 1.0;
    gl_Position = vec4(x, y, 0.0, 1.0);
}
