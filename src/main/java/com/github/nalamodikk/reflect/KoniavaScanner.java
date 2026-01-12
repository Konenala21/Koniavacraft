package com.github.nalamodikk.reflect;

import com.github.nalamodikk.KoniavacraftMod;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Koniavacraft 反射掃描器
 * 負責在啟動時發現帶有特定註解的類
 */
public class KoniavaScanner {
    private static final Set<String> TARGET_PACKAGES = new HashSet<>();
    private static final Set<SimpleClassInfo> SCANNED_CLASSES = new HashSet<>();
    private static boolean loaded = false;

    static {
        // 預設掃描主包
        registerPackage("com.github.nalamodikk");
    }

    public static void registerPackage(String packageName) {
        TARGET_PACKAGES.add(packageName);
    }

    /**
     * 執行路徑掃描 (主要用於開發環境或 Fabric)
     */
    public static void scan() {
        if (loaded)
            return;
        loaded = true;

        long start = System.currentTimeMillis();
        KoniavacraftMod.LOGGER.info("正在執行全域掃描...");

        try (ScanResult result = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(TARGET_PACKAGES.toArray(new String[0]))
                .scan()) {

            result.getAllClasses().filter(info -> !info.getAnnotations().isEmpty()).forEach(info -> {
                Set<String> annos = info.getAnnotations().stream()
                        .map(a -> a.getName())
                        .collect(Collectors.toSet());
                SCANNED_CLASSES.add(new SimpleClassInfo(info.getName(), annos));
            });
        }

        long end = System.currentTimeMillis();
        KoniavacraftMod.LOGGER.info("掃描完成，耗時 {}ms，共發現 {} 個類別。", (end - start), SCANNED_CLASSES.size());
    }

    /**
     * 用於 NeoForge 的預處理結果輸入（整合 ModList 掃描結果）
     */
    public static void inputScanResult(SimpleClassInfo info) {
        SCANNED_CLASSES.add(info);
    }

    public static void markAsLoaded() {
        loaded = true;
    }

    public static Set<SimpleClassInfo> getWithAnnotation(Class<?> annotationClass) {
        return SCANNED_CLASSES.stream()
                .filter(c -> c.isAnnotationPresent(annotationClass))
                .collect(Collectors.toSet());
    }
}
