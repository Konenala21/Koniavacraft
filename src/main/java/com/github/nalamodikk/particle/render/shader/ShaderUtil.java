package com.github.nalamodikk.particle.render.shader;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.render.shader.data.VertexData;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShaderUtil {

    public static String readShaderSource(String path) {
        String fullPath = "assets/koniava/shaders/" + path;
        try (InputStream stream = ShaderUtil.class.getClassLoader().getResourceAsStream(fullPath)) {
            if (stream == null) {
                KoniavacraftMod.LOGGER.error("Shader file not found: {}", fullPath);
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("Failed to read shader: {}", fullPath, e);
            return "";
        }
    }

    public static String readShaderSource(ResourceLocation location) {
        String fullPath = "assets/" + location.getNamespace() + "/shaders/" + location.getPath();
        try (InputStream stream = ShaderUtil.class.getClassLoader().getResourceAsStream(fullPath)) {
            if (stream == null) {
                 KoniavacraftMod.LOGGER.error("Shader file not found: {}", fullPath);
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
             KoniavacraftMod.LOGGER.error("Failed to read shader: {}", fullPath, e);
            return "";
        }
    }

    public static List<VertexData> genSquareUVScreen(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        List<VertexData> res = new ArrayList<>();
        res.add(new VertexData(p1, new Vector2f(0f, 1f)));
        res.add(new VertexData(p2, new Vector2f(1f, 1f)));
        res.add(new VertexData(p4, new Vector2f(0f, 0f)));

        res.add(new VertexData(p2, new Vector2f(1f, 1f)));
        res.add(new VertexData(p3, new Vector2f(1f, 0f)));
        res.add(new VertexData(p4, new Vector2f(0f, 0f)));
        return res;
    }
}
