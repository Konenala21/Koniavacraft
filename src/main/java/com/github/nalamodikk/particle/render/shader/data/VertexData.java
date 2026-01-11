package com.github.nalamodikk.particle.render.shader.data;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class VertexData {
    public Vector3f pos;
    public Vector4f color;
    public Vector2f uv;

    public VertexData(Vector3f pos, Vector4f color, Vector2f uv) {
        this.pos = pos;
        this.color = color;
        this.uv = uv;
    }

    public VertexData(Vector3f pos) {
        this(pos, new Vector4f(1, 1, 1, 1), new Vector2f(0, 0));
    }

    public VertexData(Vector3f pos, Vector2f uv) {
        this(pos, new Vector4f(1, 1, 1, 1), uv);
    }

    public VertexData(Vector3f pos, Vector4f color) {
        this(pos, color, new Vector2f(0, 0));
    }
}
