package com.github.nalamodikk.particle.render.shader.vertex;

import com.github.nalamodikk.particle.render.shader.ShaderUtil;
import com.github.nalamodikk.particle.render.shader.data.CooVertexFormat;
import org.joml.Vector3f;

public class VertexBuffers {

    public static SimpleVertexBuffer getScreenBuffer() {
        SimpleVertexBuffer buffer = new SimpleVertexBuffer();
        buffer.setVertexes(
            ShaderUtil.genSquareUVScreen(
                new Vector3f(-1f, 1f, 0f),
                new Vector3f(1f, 1f, 0f),
                new Vector3f(1f, -1f, 0f),
                new Vector3f(-1f, -1f, 0f)
            ), 
            CooVertexFormat.POINT_TEXTURE_UV_FORMAT
        );
        return buffer;
    }
}
