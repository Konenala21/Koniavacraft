package com.github.nalamodikk.client.screenAPI.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 纹理信息自动检测工具
 *
 * 功能：
 * - 自动读取 PNG 文件的实际尺寸
 * - 缓存结果，避免重复读取
 * - 失败时返回默认值
 *
 * 参考 LDLib2 的资源自动检测概念
 */
public class TextureInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureInfo.class);
    private static final Map<ResourceLocation, Size> SIZE_CACHE = new HashMap<>();

    /**
     * 纹理尺寸记录
     */
    public record Size(int width, int height) {
        public static final Size DEFAULT = new Size(256, 256);
    }

    /**
     * 获取纹理的实际尺寸（带缓存）
     *
     * @param texture 纹理资源位置
     * @return 纹理尺寸，失败时返回 256×256
     */
    public static Size getSize(ResourceLocation texture) {
        return SIZE_CACHE.computeIfAbsent(texture, TextureInfo::loadSize);
    }

    /**
     * 从资源管理器加载纹理尺寸
     */
    private static Size loadSize(ResourceLocation texture) {
        try {
            Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(texture)
                .orElseThrow(() -> new IOException("Resource not found: " + texture));

            try (InputStream stream = resource.open()) {
                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    LOGGER.warn("Failed to read image: {}", texture);
                    return Size.DEFAULT;
                }

                int width = image.getWidth();
                int height = image.getHeight();

                LOGGER.debug("Loaded texture size: {} = {}×{}", texture, width, height);
                return new Size(width, height);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load texture size for {}: {}", texture, e.getMessage());
            return Size.DEFAULT;
        }
    }

    /**
     * 清除缓存（用于资源重载）
     */
    public static void clearCache() {
        SIZE_CACHE.clear();
        LOGGER.debug("Texture size cache cleared");
    }

    /**
     * 预加载纹理尺寸（可选的优化）
     */
    public static void preload(ResourceLocation texture) {
        getSize(texture);
    }
}
