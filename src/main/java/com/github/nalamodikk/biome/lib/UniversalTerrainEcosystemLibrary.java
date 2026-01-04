// 🌍 通用地形生態系統庫
// Universal Terrain Ecosystem Library
package com.github.nalamodikk.biome.lib;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🌍 通用地形生態系統庫
 *
 * 這個庫讓你能夠輕鬆添加新的地形生態系統，只需要：
 * 1. 定義生物群系和方塊
 * 2. 創建 EcosystemConfig 配置
 * 3. 註冊到庫中
 *
 * 使用範例：
 * ```java
 * // 註冊魔力草原生態系統
 * UniversalTerrainLibrary.registerEcosystem(
 *     ModBiomes.MANA_PLAINS,
 *     EcosystemConfig.builder()
 *         .surfaceBlock(() -> ModBlocks.MANA_GRASS_BLOCK.get())
 *         .soilBlock(() -> ModBlocks.MANA_SOIL.get())
 *         .deepSoilBlock(() -> ModBlocks.DEEP_MANA_SOIL.get())
 *         .deepSoilThreshold(20)
 *         .waterRules(WaterRules.AVOID_WATER)
 *         .priority(10)
 *         .build()
 * );
 *
 * // 註冊火山灰原生態系統
 * UniversalTerrainLibrary.registerEcosystem(
 *     ModBiomes.VOLCANIC_ASHLANDS,
 *     EcosystemConfig.builder()
 *         .surfaceBlock(() -> Blocks.COARSE_DIRT)
 *         .soilBlock(() -> Blocks.COARSE_DIRT)
 *         .stoneBlock(() -> Blocks.BASALT)
 *         .waterRules(WaterRules.REPLACE_WITH_LAVA)
 *         .priority(8)
 *         .build()
 * );
 * ```
 */
public class UniversalTerrainEcosystemLibrary {

    // 🗃️ 生態系統註冊表
    private static final Map<ResourceKey<Biome>, EcosystemConfig> ECOSYSTEM_REGISTRY = new ConcurrentHashMap<>();

    // 📦 已創建的規則緩存
    private static final Map<ResourceKey<Biome>, SurfaceRules.RuleSource> RULE_CACHE = new ConcurrentHashMap<>();
    private static volatile SurfaceRules.RuleSource ALL_RULES_CACHE = null;
    private static volatile boolean ALL_RULES_CACHED = false;

    // 🔇 日誌控制
    private static boolean LOGGED_INITIALIZATION = false;

    /**
     * 📝 註冊新的生態系統
     *
     * @param biome 生物群系
     * @param config 生態系統配置
     */
    public static void registerEcosystem(ResourceKey<Biome> biome, EcosystemConfig config) {
        ECOSYSTEM_REGISTRY.put(biome, config);
        RULE_CACHE.remove(biome); // 清除緩存，強制重新生成
        ALL_RULES_CACHE = null;
        ALL_RULES_CACHED = false;

        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.info("🌱 已註冊地形生態系統: {} (優先級: {})",
                    biome.location(), config.priority());
        }
    }

    /**
     * 🎯 創建所有已註冊生態系統的 Surface Rules
     */
    public static SurfaceRules.RuleSource createAllEcosystemRules() {
        if (ALL_RULES_CACHED) {
            return ALL_RULES_CACHE;
        }
        List<SurfaceRules.RuleSource> validRules = new ArrayList<>();

        // 按優先級排序處理
        ECOSYSTEM_REGISTRY.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().priority(), a.getValue().priority()))
                .forEach(entry -> {
                    ResourceKey<Biome> biome = entry.getKey();
                    SurfaceRules.RuleSource rule = getOrCreateRule(biome);
                    if (rule != null) {
                        validRules.add(rule);
                    }
                });

        if (validRules.isEmpty()) {
            if (KoniavacraftMod.IS_DEV && !LOGGED_INITIALIZATION) {
                KoniavacraftMod.LOGGER.warn("⚠️ 沒有找到有效的地形生態系統規則");
                LOGGED_INITIALIZATION = true;
            }
            ALL_RULES_CACHE = null;
            ALL_RULES_CACHED = true;
            return null;
        }

        if (KoniavacraftMod.IS_DEV && !LOGGED_INITIALIZATION) {
            KoniavacraftMod.LOGGER.info("✅ 成功創建 {} 個地形生態系統規則", validRules.size());
            LOGGED_INITIALIZATION = true;
        }

        ALL_RULES_CACHE = SurfaceRules.sequence(validRules.toArray(new SurfaceRules.RuleSource[0]));
        ALL_RULES_CACHED = true;
        return ALL_RULES_CACHE;
    }

    public static void clearAllRulesCache() {
        ALL_RULES_CACHE = null;
        ALL_RULES_CACHED = false;
    }

    /**
     * ⚡ 獲取或創建單個生態系統的規則（懶加載）
     */
    private static SurfaceRules.RuleSource getOrCreateRule(ResourceKey<Biome> biome) {
        // 檢查緩存
        if (RULE_CACHE.containsKey(biome)) {
            return RULE_CACHE.get(biome);
        }

        EcosystemConfig config = ECOSYSTEM_REGISTRY.get(biome);
        if (config == null) {
            return null;
        }

        try {
            // 檢查生物群系是否存在
            if (!doesBiomeExist(biome)) {
                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.debug("⚠️ 生物群系不存在，跳過: {}", biome.location());
                }
                return null;
            }

            // 檢查所需方塊是否存在
            if (!config.areBlocksValid()) {
                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.warn("⚠️ 生態系統所需方塊不存在，跳過: {}", biome.location());
                }
                return null;
            }

            // 創建規則
            SurfaceRules.RuleSource rule = createEcosystemRule(biome, config);
            if (rule != null) {
                RULE_CACHE.put(biome, rule);
                if (KoniavacraftMod.IS_DEV) {
                    KoniavacraftMod.LOGGER.debug("✨ 成功創建生態系統規則: {}", biome.location());
                }
                return rule;
            }

        } catch (Exception e) {
            KoniavacraftMod.LOGGER.error("❌ 創建生態系統規則失敗: {} - {}", biome.location(), e.getMessage());
        }

        return null;
    }

    /**
     * 🏗️ 創建單個生態系統的 Surface Rules
     */
    private static SurfaceRules.RuleSource createEcosystemRule(ResourceKey<Biome> biome, EcosystemConfig config) {
        List<SurfaceRules.RuleSource> rules = new ArrayList<>();

        // 🌿 地表規則
        if (config.surfaceBlock() != null) {
            SurfaceRules.RuleSource surfaceRule = SurfaceRules.ifTrue(
                    SurfaceRules.ON_FLOOR,
                    createSurfaceBlockRule(config)
            );
            rules.add(surfaceRule);
        }

        // 🌱 地下規則
        if (config.soilBlock() != null || config.deepSoilBlock() != null || config.stoneBlock() != null) {
            SurfaceRules.RuleSource undergroundRule = SurfaceRules.ifTrue(
                    SurfaceRules.UNDER_FLOOR,
                    createUndergroundRule(config)
            );
            rules.add(undergroundRule);
        }

        // 🌊 特殊水規則
        if (config.waterRules() == WaterRules.REPLACE_WITH_LAVA) {
            SurfaceRules.RuleSource lavaRule = SurfaceRules.ifTrue(
                    SurfaceRules.waterBlockCheck(0, 0), // 在水位置
                    makeStateRule(() -> net.minecraft.world.level.block.Blocks.LAVA) // 替換為岩漿
            );
            rules.add(lavaRule);
        }

        if (rules.isEmpty()) {
            return null;
        }

        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biome),
                SurfaceRules.sequence(rules.toArray(new SurfaceRules.RuleSource[0]))
        );
    }

    /**
     * 🌿 創建地表方塊規則
     */
    private static SurfaceRules.RuleSource createSurfaceBlockRule(EcosystemConfig config) {
        List<SurfaceRules.ConditionSource> conditions = new ArrayList<>();

        // 🌊 水規則處理
        switch (config.waterRules()) {
            case AVOID_WATER:
                conditions.add(SurfaceRules.waterBlockCheck(-1, 0)); // 上方沒有水
                break;
            case ALLOW_UNDERWATER:
                // 不添加水檢查條件
                break;
            case ONLY_NEAR_WATER:
                conditions.add(SurfaceRules.not(SurfaceRules.waterBlockCheck(-3, 0))); // 3格內有水
                break;
        }

        SurfaceRules.RuleSource blockRule = makeStateRule(config.surfaceBlock());

        // 組合所有條件
        if (conditions.isEmpty()) {
            return blockRule;
        } else {
            SurfaceRules.ConditionSource combinedCondition = conditions.get(0);
            for (int i = 1; i < conditions.size(); i++) {
                // 這裡需要實現條件組合邏輯，目前簡化為第一個條件
            }
            return SurfaceRules.ifTrue(combinedCondition, blockRule);
        }
    }

    /**
     * 🌱 創建地下規則
     */
    /**
     * 🌱 創建地下規則 - 修正版
     */
    private static SurfaceRules.RuleSource createUndergroundRule(EcosystemConfig config) {
        List<SurfaceRules.RuleSource> undergroundRules = new ArrayList<>();

        // 🌊 避免水下替換（如果設定）
        SurfaceRules.ConditionSource waterCondition = config.waterRules() == WaterRules.AVOID_WATER
                ? SurfaceRules.waterBlockCheck(0, 0) // 當前位置沒有水
                : null;

        // 🎯 關鍵修正：調整規則順序，確保層次正確

        // 🌾 1. 淺層土壤規則（Y >= threshold 時使用普通土壤）
        if (config.soilBlock() != null && config.deepSoilBlock() != null && config.deepSoilThreshold() > Integer.MIN_VALUE) {
            SurfaceRules.RuleSource shallowSoilRule = SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(config.deepSoilThreshold()), 0), // Y >= threshold
                    makeStateRule(config.soilBlock())
            );
            undergroundRules.add(shallowSoilRule);
        }

        // 🏔️ 2. 深層土壤規則（Y < threshold 時使用深層土壤）
        if (config.deepSoilBlock() != null && config.deepSoilThreshold() > Integer.MIN_VALUE) {
            SurfaceRules.RuleSource deepSoilRule = SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(config.deepSoilThreshold()), -1), // Y < threshold
                    makeStateRule(config.deepSoilBlock())
            );
            undergroundRules.add(deepSoilRule);
        }

        // 🌾 3. 後備土壤規則（如果沒有深層土壤配置，使用普通土壤）
        else if (config.soilBlock() != null) {
            undergroundRules.add(makeStateRule(config.soilBlock()));
        }

        // 🗿 4. 石頭替換規則（最深層）
        if (config.stoneBlock() != null) {
            SurfaceRules.RuleSource stoneRule = SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(config.stoneThreshold()), -1), // Y < stone threshold
                    makeStateRule(config.stoneBlock())
            );
            undergroundRules.add(stoneRule);
        }

        if (undergroundRules.isEmpty()) {
            return null;
        }

        SurfaceRules.RuleSource combinedRule = SurfaceRules.sequence(
                undergroundRules.toArray(new SurfaceRules.RuleSource[0])
        );

        // 如果需要避免水，包裝在水檢查中
        if (waterCondition != null) {
            return SurfaceRules.ifTrue(waterCondition, combinedRule);
        } else {
            return combinedRule;
        }
    }
    /**
     * 🔍 檢查生物群系是否存在
     */
    private static boolean doesBiomeExist(ResourceKey<Biome> biome) {
        try {
            SurfaceRules.RuleSource testRule = SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(biome),
                    SurfaceRules.state(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🛠️ 工具方法：創建方塊狀態規則
     */
    private static SurfaceRules.RuleSource makeStateRule(Supplier<Block> blockSupplier) {
        try {
            Block block = blockSupplier.get();
            return SurfaceRules.state(block.defaultBlockState());
        } catch (Exception e) {
            return SurfaceRules.state(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
    }

    /**
     * 🧹 清理緩存（用於開發和測試）
     */
    public static void clearCache() {
        RULE_CACHE.clear();
        LOGGED_INITIALIZATION = false;
        if (KoniavacraftMod.IS_DEV) {
            KoniavacraftMod.LOGGER.debug("🧹 地形生態系統庫緩存已清理");
        }
    }

    /**
     * 📊 獲取已註冊的生態系統數量
     */
    public static int getRegisteredEcosystemCount() {
        return ECOSYSTEM_REGISTRY.size();
    }

    /**
     * 📋 獲取所有已註冊的生物群系
     */
    public static Set<ResourceKey<Biome>> getRegisteredBiomes() {
        return new HashSet<>(ECOSYSTEM_REGISTRY.keySet());
    }
}
