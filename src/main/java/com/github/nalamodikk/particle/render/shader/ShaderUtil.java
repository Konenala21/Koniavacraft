package com.github.nalamodikk.particle.render.shader;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
}
