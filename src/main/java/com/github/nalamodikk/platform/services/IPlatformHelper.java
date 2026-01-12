package com.github.nalamodikk.platform.services;

/**
 * 平台功能接口
 * 用於隔離不同平台（如 NeoForge, Fabric）的 API
 */
public interface IPlatformHelper {
    /**
     * 獲取當前平台名稱
     */
    String getPlatformName();

    /**
     * 檢查特定 Mod 是否已載入
     */
    boolean isModLoaded(String modId);

    /**
     * 檢查是否為開發環境
     */
    boolean isDevelopmentEnvironment();
}
