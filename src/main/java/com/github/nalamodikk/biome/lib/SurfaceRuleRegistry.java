package com.github.nalamodikk.biome.lib;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SurfaceRule registration with namespace + stage + priority semantics.
 */
public final class SurfaceRuleRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<RuleCategory, Map<RuleStage, Map<String, RuleEntry>>> REGISTRY = new EnumMap<>(RuleCategory.class);

    private SurfaceRuleRegistry() {
    }

    public static void register(RuleCategory category, RuleStage stage, String namespace, int priority, Supplier<SurfaceRules.RuleSource> supplier) {
        REGISTRY
                .computeIfAbsent(category, key -> createStageMap())
                .computeIfAbsent(stage, key -> new ConcurrentHashMap<>())
                .put(namespace, new RuleEntry(namespace, priority, supplier));
    }

    public static SurfaceRules.RuleSource build(RuleCategory category, RuleStage stage) {
        Map<RuleStage, Map<String, RuleEntry>> byStage = REGISTRY.get(category);
        if (byStage == null) {
            return null;
        }
        Map<String, RuleEntry> entries = byStage.get(stage);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        List<SurfaceRules.RuleSource> resolvedRules = new ArrayList<>();
        List<RuleEntry> sortedEntries = new ArrayList<>(entries.values());
        sortedEntries.sort(Comparator
                .comparingInt(RuleEntry::priority).reversed()
                .thenComparing(RuleEntry::namespace));

        for (RuleEntry entry : sortedEntries) {
            try {
                SurfaceRules.RuleSource ruleSource = entry.supplier().get();
                if (ruleSource != null) {
                    resolvedRules.add(ruleSource);
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to resolve surface rule supplier for namespace {}", entry.namespace(), exception);
            }
        }

        if (resolvedRules.isEmpty()) {
            return null;
        }
        return SurfaceRules.sequence(resolvedRules.toArray(new SurfaceRules.RuleSource[0]));
    }

    public static void clear() {
        REGISTRY.clear();
    }

    private static Map<RuleStage, Map<String, RuleEntry>> createStageMap() {
        Map<RuleStage, Map<String, RuleEntry>> map = new EnumMap<>(RuleStage.class);
        for (RuleStage stage : RuleStage.values()) {
            map.put(stage, new ConcurrentHashMap<>());
        }
        return map;
    }

    public enum RuleCategory {
        OVERWORLD,
        NETHER,
        END
    }

    public enum RuleStage {
        BEFORE_VANILLA,
        AFTER_VANILLA
    }

    private record RuleEntry(
            String namespace,
            int priority,
            Supplier<SurfaceRules.RuleSource> supplier
    ) {
    }
}
