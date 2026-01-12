package com.github.nalamodikk.render.shader.utils;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * GLSL 工具類
 * 負責從資源路徑讀取 Shader 源碼
 */
public class GlslUtil {

    public static String readShader(ResourceLocation id) {
        String path = "/assets/" + id.getNamespace() + "/shaders/" + id.getPath();
        try (InputStream is = KoniavacraftMod.class.getResourceAsStream(path)) {
            if (is == null)
                throw new RuntimeException("找不到 Shader 檔案: " + path);
            return new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("讀取 Shader 出錯: " + id, e);
            return "";
        }
    }
}
