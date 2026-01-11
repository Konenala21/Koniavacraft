package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.render.shader.ShaderProgramBuilder;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShaderCompileTest {

    @Test
    public void testBuilderStructure() {
        // 這只測試 Java 類別結構是否正確，不會真的呼叫 OpenGL (因為不在遊戲環境)
        ShaderProgramBuilder builder = new ShaderProgramBuilder();
        assertNotNull(builder);
        
        // 驗證方法是否存在
        assertThrows(IllegalStateException.class, builder::build);
    }
}
