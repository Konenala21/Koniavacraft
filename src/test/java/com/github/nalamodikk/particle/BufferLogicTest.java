package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.render.shader.data.CooVertexFormat;
import com.github.nalamodikk.particle.render.shader.data.VertexData;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BufferLogicTest {

    @Test
    public void testVertexDataStructure() {
        Vector3f pos = new Vector3f(1, 2, 3);
        VertexData data = new VertexData(pos);
        assertEquals(1.0f, data.pos.x);
        assertEquals(2.0f, data.pos.y);
        assertEquals(3.0f, data.pos.z);
    }

    @Test
    public void testVertexFormat() {
        assertEquals(3, CooVertexFormat.POINT_FORMAT.getElementsPerVertex());
        assertEquals(7, CooVertexFormat.POINT_COLOR_FORMAT.getElementsPerVertex());
    }
}
