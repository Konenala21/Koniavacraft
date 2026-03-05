package com.github.nalamodikk.biome.data;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.region.BiomeClimateDefinition;
import com.github.nalamodikk.biome.region.BiomeInjectionEntry;
import com.github.nalamodikk.biome.region.VanillaClimateBands;
import net.minecraft.world.level.biome.Climate;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class BiomeClimateConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeClimateConfigLoader.class);
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_WEIGHT = 5;
    private static final int DEFAULT_PRIORITY = 0;
    private static final String FOLDER = "worldgen/biome_climates";

    private static final Map<ResourceKey<Biome>, OverrideData> OVERRIDES = new ConcurrentHashMap<>();
    private static final List<AdditionalData> ADDITIONAL_ENTRIES = new CopyOnWriteArrayList<>();
    private static final Set<ResourceKey<Biome>> DISABLED_BIOMES = ConcurrentHashMap.newKeySet();

    public BiomeClimateConfigLoader() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BiomeClimateConfigLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        OVERRIDES.clear();
        ADDITIONAL_ENTRIES.clear();
        DISABLED_BIOMES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "biome_climate");
                parseOne(fileId, json);
            } catch (Exception exception) {
                LOGGER.error("Failed to parse biome climate config: {}", fileId, exception);
            }
        }

        LOGGER.info(
                "Loaded {} biome climate overrides, {} additional biome injections, {} disabled biomes from datapacks",
                OVERRIDES.size(),
                ADDITIONAL_ENTRIES.size(),
                DISABLED_BIOMES.size()
        );
    }

    public static BiomeInjectionEntry resolveOverride(BiomeInjectionEntry baseEntry) {
        OverrideData overrideData = OVERRIDES.get(baseEntry.biome());
        if (overrideData == null) {
            return baseEntry;
        }

        BiomeInjectionEntry resolved = baseEntry;
        if (overrideData.climate() != null) {
            resolved = resolved.withClimate(overrideData.climate());
        }
        if (overrideData.weight() != null) {
            resolved = resolved.withWeight(overrideData.weight());
        }
        if (overrideData.description() != null) {
            resolved = resolved.withDescription(overrideData.description());
        }
        if (overrideData.priority() != null) {
            resolved = resolved.withPriority(overrideData.priority());
        }
        if (overrideData.sourceNamespace() != null && !overrideData.sourceNamespace().isBlank()) {
            resolved = resolved.withNamespace(overrideData.sourceNamespace());
        }
        return resolved;
    }

    public static boolean isDisabled(ResourceKey<Biome> biome) {
        return DISABLED_BIOMES.contains(biome);
    }

    public static List<BiomeInjectionEntry> getAdditionalEntries(Set<ResourceKey<Biome>> knownBiomes) {
        List<BiomeInjectionEntry> result = new ArrayList<>();
        for (AdditionalData additionalData : ADDITIONAL_ENTRIES) {
            boolean exists = knownBiomes.contains(additionalData.entry().biome());
            if (exists && !additionalData.allowExistingBiome()) {
                continue;
            }
            result.add(additionalData.entry());
        }
        return result;
    }

    private static void parseOne(ResourceLocation fileId, JsonObject json) {
        if (!GsonHelper.getAsBoolean(json, "enabled", true)) {
            return;
        }

        String biomeString = GsonHelper.getAsString(json, "biome", "");
        if (biomeString.isBlank()) {
            LOGGER.warn("Skipped biome climate config {} because field 'biome' is missing", fileId);
            return;
        }

        ResourceLocation biomeLocation = ResourceLocation.tryParse(biomeString);
        if (biomeLocation == null) {
            LOGGER.warn("Skipped biome climate config {} because biome id is invalid: {}", fileId, biomeString);
            return;
        }
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeLocation);

        String mode = GsonHelper.getAsString(json, "mode", "override").toLowerCase();

        if ("disable".equals(mode)) {
            DISABLED_BIOMES.add(biomeKey);
            LOGGER.info("Biome climate disabled by datapack: {}", biomeKey.location());
            return;
        }

        String namespace = GsonHelper.getAsString(json, "namespace", fileId.getNamespace());
        Integer weight = json.has("weight") ? GsonHelper.getAsInt(json, "weight") : null;
        Integer priority = json.has("priority") ? GsonHelper.getAsInt(json, "priority") : null;
        String description = json.has("description") ? GsonHelper.getAsString(json, "description") : null;
        BiomeClimateDefinition climate = parseClimateDefinition(json);

        if ("append".equals(mode)) {
            if (climate == null) {
                LOGGER.warn("Skipped append biome climate config {} because no climate range was provided", fileId);
                return;
            }
            int resolvedWeight = weight != null ? weight : DEFAULT_WEIGHT;
            int resolvedPriority = priority != null ? priority : DEFAULT_PRIORITY;
            boolean allowExistingBiome = GsonHelper.getAsBoolean(json, "allow_existing_biome", false);
            BiomeInjectionEntry entry = new BiomeInjectionEntry(
                    biomeKey,
                    climate,
                    resolvedWeight,
                    description == null ? "" : description,
                    resolvedPriority,
                    namespace
            );
            ADDITIONAL_ENTRIES.add(new AdditionalData(entry, allowExistingBiome));
            return;
        }

        OVERRIDES.put(biomeKey, new OverrideData(climate, weight, description, priority, namespace));
    }

    private static BiomeClimateDefinition parseClimateDefinition(JsonObject json) {
        boolean hasAnyRange = json.has("temperature")
                || json.has("humidity")
                || json.has("continentalness")
                || json.has("erosion")
                || json.has("depth")
                || json.has("weirdness")
                || json.has("offset");

        if (!hasAnyRange) {
            return null;
        }

        BiomeClimateDefinition.Builder builder = BiomeClimateDefinition.builder();
        applyRange(json, "temperature", builder::temperature);
        applyRange(json, "humidity", builder::humidity);
        applyRange(json, "continentalness", builder::continentalness);
        applyRange(json, "erosion", builder::erosion);
        applyRange(json, "depth", builder::depth);
        applyRange(json, "weirdness", builder::weirdness);
        if (json.has("offset")) {
            builder.offset(GsonHelper.getAsFloat(json, "offset"));
        }
        return builder.build();
    }

    private static void applyRange(JsonObject json, String key, RangeSetter setter) {
        if (!json.has(key)) {
            return;
        }
        JsonObject range = GsonHelper.getAsJsonObject(json, key);

        // 新格式：band 名稱（優先判斷）
        if (range.has("band")) {
            String bandName = GsonHelper.getAsString(range, "band");
            Climate.Parameter p = VanillaClimateBands.parseParameter(key, bandName);
            if (p != null) {
                setter.apply(Climate.unquantizeCoord(p.min()), Climate.unquantizeCoord(p.max()));
            } else {
                LOGGER.warn("Skipped '{}' band '{}': unknown band name", key, bandName);
            }
            return;
        }

        // 新格式：band_min + band_max span
        if (range.has("band_min") && range.has("band_max")) {
            String bandMin = GsonHelper.getAsString(range, "band_min");
            String bandMax = GsonHelper.getAsString(range, "band_max");
            Climate.Parameter p = VanillaClimateBands.parseSpan(key, bandMin, bandMax);
            if (p != null) {
                setter.apply(Climate.unquantizeCoord(p.min()), Climate.unquantizeCoord(p.max()));
            } else {
                LOGGER.warn("Skipped '{}' band_min='{}' band_max='{}': parse failed", key, bandMin, bandMax);
            }
            return;
        }

        // 舊格式：min/max 數值（完全向下相容）
        float min = GsonHelper.getAsFloat(range, "min");
        float max = GsonHelper.getAsFloat(range, "max");
        setter.apply(min, max);
    }

    private interface RangeSetter {
        void apply(float min, float max);
    }

    private record OverrideData(
            BiomeClimateDefinition climate,
            Integer weight,
            String description,
            Integer priority,
            String sourceNamespace
    ) {
    }

    private record AdditionalData(
            BiomeInjectionEntry entry,
            boolean allowExistingBiome
    ) {
    }
}
