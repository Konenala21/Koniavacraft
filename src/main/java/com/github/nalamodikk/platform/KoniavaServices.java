package com.github.nalamodikk.platform;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * 服務載入器
 * 使用 Java ServiceLoader 機制在運行時加載平台實作
 */
public class KoniavaServices {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * 加載指定的服務實作
     * 實作類必須在 META-INF/services/ 中註冊
     */
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("無法加載服務實作: " + clazz.getName()));
        KoniavacraftMod.LOGGER.debug("已加載服務: {} -> {}", clazz.getSimpleName(), loadedService.getClass().getName());
        return loadedService;
    }
}
