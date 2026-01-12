package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.render.shader.ShaderProgramBuilder;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShaderCompileTest {

    @Test
    public void testBuilderStructure() {
        // ?皜祈岫 Java 憿蝯??臬甇?Ⅱ嚗??????OpenGL (?銝??啣?)
        ShaderProgramBuilder builder = new ShaderProgramBuilder();
        assertNotNull(builder);
        
        // 撽??寞??臬摮
        assertThrows(IllegalStateException.class, builder::build);
    }
}
