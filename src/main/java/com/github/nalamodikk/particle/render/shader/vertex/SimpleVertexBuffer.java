package com.github.nalamodikk.particle.render.shader.vertex;

import com.github.nalamodikk.particle.render.shader.api.VertexBuffer;
import com.github.nalamodikk.particle.render.shader.data.CooVertexFormat;
import com.github.nalamodikk.particle.render.shader.data.VertexData;
import org.lwjgl.opengl.GL33;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class SimpleVertexBuffer implements VertexBuffer {
    private int vao = 0;
    private int vbo = 0;
    private int lastVAO = 0;
    private CooVertexFormat currentVertexFormat = CooVertexFormat.POINT_FORMAT;
    private int drawMode = GL33.GL_TRIANGLES;
    private final List<VertexData> vertexes = new ArrayList<>();
    private boolean dirty = false;
    private int uploadedProgram = 0;

    public int getVao() { return vao; }
    public int getVbo() { return vbo; }

    @Override
    public void uploadVertexes() {
        int programNow = GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM);
        boolean updateProgram = programNow != uploadedProgram;
        
        if (dirty || updateProgram) {
            int currentVAO = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
            boolean needReset = false;
            if (currentVAO != vao) {
                use();
                needReset = true;
            }
            upload();
            if (needReset) {
                reset();
            }
            dirty = false;
        }
        if (updateProgram) {
            uploadedProgram = programNow;
        }
    }

    private void upload() {
        float[] data = vertexToData();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, data, GL33.GL_STATIC_DRAW);
        
        int offset = (int) dataOffsetCount() * Float.BYTES;
        
        // Position
        GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, offset, 0L);
        GL33.glEnableVertexAttribArray(0);
        
        if (currentVertexFormat == CooVertexFormat.POINT_COLOR_TEXTURE_UV_FORMAT) {
            // Color
            GL33.glVertexAttribPointer(1, 4, GL33.GL_FLOAT, false, offset, 3L * Float.BYTES);
            GL33.glEnableVertexAttribArray(1);
            // UV
            GL33.glVertexAttribPointer(2, 2, GL33.GL_FLOAT, false, offset, 7L * Float.BYTES);
            GL33.glEnableVertexAttribArray(2);
        } else if (currentVertexFormat != CooVertexFormat.POINT_FORMAT) {
            int size = (currentVertexFormat == CooVertexFormat.POINT_COLOR_FORMAT) ? 4 : 2;
            GL33.glVertexAttribPointer(1, size, GL33.GL_FLOAT, false, offset, 3L * Float.BYTES);
            GL33.glEnableVertexAttribArray(1);
        }
    }

    @Override
    public void setVertexes(List<VertexData> vertexes, CooVertexFormat format) {
        this.vertexes.clear();
        this.vertexes.addAll(vertexes);
        this.currentVertexFormat = format;
        this.dirty = true;
    }

    @Override
    public void draw() {
        use();
        uploadVertexes();
        GL33.glDrawArrays(drawMode, 0, vertexes.size());
        reset();
    }

    @Override
    public void init() {
        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();
        use();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        reset();
    }

    @Override
    public void use() {
        lastVAO = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
        GL33.glBindVertexArray(vao);
    }

    @Override
    public void reset() {
        GL33.glBindVertexArray(lastVAO);
    }

    @Override
    public void release() {
        GL33.glDeleteVertexArrays(vao);
        GL33.glDeleteBuffers(vbo);
    }

    private long dataOffsetCount() {
        switch (currentVertexFormat) {
            case POINT_COLOR_FORMAT: return 7;
            case POINT_TEXTURE_UV_FORMAT: return 5;
            case POINT_FORMAT: return 3;
            case POINT_COLOR_TEXTURE_UV_FORMAT: return 9;
            default: return 3;
        }
    }

    private float[] vertexToData() {
        int count = vertexes.size();
        if (count <= 0) return new float[0];
        
        int offsetCount = (int) dataOffsetCount();
        float[] res = new float[count * offsetCount];
        
        for (int i = 0; i < vertexes.size(); i++) {
            VertexData vertex = vertexes.get(i);
            Vector3f pos = vertex.pos;
            Vector4f color = vertex.color;
            Vector2f uv = vertex.uv;
            
            int base = i * offsetCount;
            
            switch (currentVertexFormat) {
                case POINT_TEXTURE_UV_FORMAT:
                    res[base] = pos.x;
                    res[base + 1] = pos.y;
                    res[base + 2] = pos.z;
                    res[base + 3] = uv.x;
                    res[base + 4] = uv.y;
                    break;
                case POINT_FORMAT:
                    res[base] = pos.x;
                    res[base + 1] = pos.y;
                    res[base + 2] = pos.z;
                    break;
                case POINT_COLOR_FORMAT:
                    res[base] = pos.x;
                    res[base + 1] = pos.y;
                    res[base + 2] = pos.z;
                    res[base + 3] = color.x;
                    res[base + 4] = color.y;
                    res[base + 5] = color.z;
                    res[base + 6] = color.w;
                    break;
                case POINT_COLOR_TEXTURE_UV_FORMAT:
                    res[base] = pos.x;
                    res[base + 1] = pos.y;
                    res[base + 2] = pos.z;
                    res[base + 3] = color.x;
                    res[base + 4] = color.y;
                    res[base + 5] = color.z;
                    res[base + 6] = color.w;
                    res[base + 7] = uv.x;
                    res[base + 8] = uv.y;
                    break;
            }
        }
        return res;
    }
}
