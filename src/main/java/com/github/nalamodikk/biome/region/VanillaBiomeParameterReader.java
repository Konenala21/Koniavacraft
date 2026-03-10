package com.github.nalamodikk.biome.region;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 從 OverworldBiomeBuilder 讀取原版 biome 的所有 Climate.ParameterPoint，
 * 實現類似 TerraBlender addBiomeSimilar 的功能。
 *
 * <p>使用方式：
 * <pre>
 * // 讓 MANA_PLAINS 出現在所有原版 PLAINS 出現的 climate 位置
 * List&lt;BiomeInjectionEntry&gt; entries = VanillaBiomeParameterReader.addBiomeSimilar(
 *     Biomes.PLAINS, ModBiomes.MANA_PLAINS, 5, 50, "koniava");
 * entries.forEach(region::registerEntry);
 * </pre>
 *
 * <p>使用 ThreadLocal 旗標防止與 OverworldBiomeBuilderMixin 之間的遞迴呼叫。
 */
public final class VanillaBiomeParameterReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaBiomeParameterReader.class);

    /** ThreadLocal 旗標：當前執行緒是否正在建立 vanilla points 快取，防止 Mixin 遞迴 */
    public static final ThreadLocal<Boolean> IS_READING_VANILLA = ThreadLocal.withInitial(() -> false);

    /** 延遲初始化的原版 biome → parameter points 快取 */
    private static volatile Map<ResourceKey<Biome>, List<Climate.ParameterPoint>> VANILLA_POINTS = null;

    private VanillaBiomeParameterReader() {}

    /**
     * 取得指定原版 biome 的所有 Climate.ParameterPoint。
     *
     * @param vanillaBiome 原版 biome key（如 Biomes.PLAINS）
     * @return 對應的 ParameterPoint 列表，若不存在則回傳空列表
     */
    public static List<Climate.ParameterPoint> getPointsFor(ResourceKey<Biome> vanillaBiome) {
        return getVanillaPoints().getOrDefault(vanillaBiome, List.of());
    }

    /**
     * 建立與指定原版 biome 相同 climate 空間的 BiomeInjectionEntry 列表。
     * 每個原版 ParameterPoint 對應一個 entry，讓自訂 biome 完全繼承原版的生成位置。
     *
     * @param vanillaBiome  要模仿的原版 biome（如 Biomes.PLAINS）
     * @param customBiome   實際要注入的自訂 biome
     * @param weight        注射權重（1-20）
     * @param priority      優先級（數字越高越優先）
     * @param namespace     來源 namespace
     * @return 可直接傳入 region::registerEntry 的列表，若找不到原版 biome 則回傳空列表
     */
    public static List<BiomeInjectionEntry> addBiomeSimilar(
            ResourceKey<Biome> vanillaBiome,
            ResourceKey<Biome> customBiome,
            int weight,
            int priority,
            String namespace) {

        List<Climate.ParameterPoint> points = getPointsFor(vanillaBiome);
        if (points.isEmpty()) {
            LOGGER.warn("addBiomeSimilar: no vanilla parameter points found for '{}'. " +
                    "Make sure it is a valid overworld biome.", vanillaBiome.location());
            return List.of();
        }

        List<BiomeInjectionEntry> entries = new ArrayList<>(points.size());
        String description = "similar_to:" + vanillaBiome.location();
        for (Climate.ParameterPoint point : points) {
            entries.add(BiomeInjectionEntry.fromPoint(
                    customBiome, point, weight, description, priority, namespace));
        }

        LOGGER.debug("addBiomeSimilar: mapped {} → {} with {} climate points",
                vanillaBiome.location(), customBiome.location(), entries.size());
        return entries;
    }

    /** 清除快取，供測試或熱重載使用 */
    public static void clearCache() {
        VANILLA_POINTS = null;
    }

    // ==================== 內部實作 ====================

    private static Map<ResourceKey<Biome>, List<Climate.ParameterPoint>> getVanillaPoints() {
        if (VANILLA_POINTS == null) {
            synchronized (VanillaBiomeParameterReader.class) {
                if (VANILLA_POINTS == null) {
                    VANILLA_POINTS = buildVanillaPointMap();
                }
            }
        }
        return VANILLA_POINTS;
    }

    /**
     * 建立原版 biome → ParameterPoint 列表的映射。
     * 設定 IS_READING_VANILLA = true 讓 OverworldBiomeBuilderMixin 跳過注入，防止遞迴。
     *
     * <p>OverworldBiomeBuilder.addBiomes() 在 1.21.1 是 protected 且類別為 final，
     * 使用 Reflection 繞過存取限制。
     */
    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<Biome>, List<Climate.ParameterPoint>> buildVanillaPointMap() {
        IS_READING_VANILLA.set(true);
        try {
            Map<ResourceKey<Biome>, List<Climate.ParameterPoint>> map = new ConcurrentHashMap<>();
            OverworldBiomeBuilder builder = new OverworldBiomeBuilder();
            Method addBiomesMethod = OverworldBiomeBuilder.class.getDeclaredMethod("addBiomes", Consumer.class);
            addBiomesMethod.setAccessible(true);
            addBiomesMethod.invoke(builder, (Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>) pair -> {
                ResourceKey<Biome> key = pair.getSecond();
                Climate.ParameterPoint point = pair.getFirst();
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(point);
            });
            LOGGER.debug("VanillaBiomeParameterReader: cached {} vanilla biome types", map.size());
            return map;
        } catch (Exception e) {
            LOGGER.error("VanillaBiomeParameterReader: failed to read vanilla biome parameter points", e);
            return new ConcurrentHashMap<>();
        } finally {
            IS_READING_VANILLA.set(false);
        }
    }
}
