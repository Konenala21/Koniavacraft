package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModBiomeJsonProvider implements DataProvider {
    private final PackOutput.PathProvider biomePathProvider;

    public ModBiomeJsonProvider(PackOutput packOutput) {
        this.biomePathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/biome");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        JsonObject biome = new JsonObject();
        biome.add("carvers", new JsonObject());
        biome.addProperty("downfall", 0.6F);
        biome.add("effects", createEffects());
        biome.add("features", createFeatures());
        biome.addProperty("has_precipitation", true);
        biome.add("spawn_costs", new JsonObject());
        biome.add("spawners", createSpawners());
        biome.addProperty("temperature", 0.7F);

        Path outputPath = this.biomePathProvider.json(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_plains")
        );
        return DataProvider.saveStable(output, biome, outputPath);
    }

    @Override
    public String getName() {
        return KoniavacraftMod.MOD_ID + " biome jsons";
    }

    private static JsonObject createEffects() {
        JsonObject effects = new JsonObject();
        effects.addProperty("fog_color", 15132410);
        effects.addProperty("foliage_color", 2142890);
        effects.addProperty("grass_color", 4251856);
        effects.add("mood_sound", createMoodSound());
        effects.add("music", createMusic());
        effects.addProperty("sky_color", 8900331);
        effects.addProperty("water_color", 4886754);
        effects.addProperty("water_fog_color", 3038170);
        return effects;
    }

    private static JsonObject createMoodSound() {
        JsonObject moodSound = new JsonObject();
        moodSound.addProperty("block_search_extent", 8);
        moodSound.addProperty("offset", 2.0);
        moodSound.addProperty("sound", "minecraft:ambient.cave");
        moodSound.addProperty("tick_delay", 6000);
        return moodSound;
    }

    private static JsonObject createMusic() {
        JsonObject music = new JsonObject();
        music.addProperty("max_delay", 24000);
        music.addProperty("min_delay", 12000);
        music.addProperty("replace_current_music", false);
        music.addProperty("sound", "minecraft:music.overworld.meadow");
        return music;
    }

    private static JsonArray createFeatures() {
        JsonArray features = new JsonArray();
        features.add(new JsonArray());
        features.add(new JsonArray());
        features.add(new JsonArray());
        features.add(arrayOf("minecraft:monster_room", "minecraft:monster_room_deep"));
        features.add(new JsonArray());
        features.add(new JsonArray());
        features.add(arrayOf(
                "minecraft:ore_coal_upper",
                "minecraft:ore_coal_lower",
                "minecraft:ore_iron_upper",
                "minecraft:ore_iron_middle",
                "minecraft:ore_iron_small",
                "minecraft:ore_gold",
                "minecraft:ore_gold_lower",
                "minecraft:ore_redstone",
                "minecraft:ore_redstone_lower",
                "minecraft:ore_diamond",
                "minecraft:ore_diamond_medium",
                "minecraft:ore_diamond_large",
                "minecraft:ore_diamond_buried",
                "minecraft:ore_lapis",
                "minecraft:ore_lapis_buried",
                "minecraft:ore_copper",
                "minecraft:underwater_magma",
                "minecraft:disk_sand",
                "minecraft:disk_clay",
                "minecraft:disk_gravel"
        ));
        features.add(new JsonArray());
        features.add(arrayOf("minecraft:spring_water", "minecraft:spring_lava"));
        features.add(arrayOf(
                "minecraft:patch_grass_badlands",
                "minecraft:patch_tall_grass_2",
                "minecraft:brown_mushroom_normal",
                "minecraft:red_mushroom_normal"
        ));
        return features;
    }

    private static JsonObject createSpawners() {
        JsonObject spawners = new JsonObject();
        spawners.add("ambient", arrayOfSpawner(entry("minecraft:bat", 10, 8, 8)));
        spawners.add("axolotls", new JsonArray());
        spawners.add("creature", arrayOfSpawner(
                entry("minecraft:cow", 8, 4, 4),
                entry("minecraft:pig", 10, 4, 4),
                entry("minecraft:sheep", 12, 4, 4),
                entry("minecraft:chicken", 10, 4, 4)
        ));
        spawners.add("misc", new JsonArray());
        spawners.add("monster", arrayOfSpawner(
                entry("minecraft:spider", 100, 4, 4),
                entry("minecraft:zombie", 95, 4, 4),
                entry("minecraft:zombie_villager", 5, 1, 1),
                entry("minecraft:skeleton", 100, 4, 4),
                entry("minecraft:creeper", 100, 4, 4),
                entry("minecraft:slime", 100, 4, 4),
                entry("minecraft:enderman", 10, 1, 4),
                entry("minecraft:witch", 5, 1, 1)
        ));
        spawners.add("underground_water_creature", arrayOfSpawner(entry("minecraft:glow_squid", 10, 4, 6)));
        spawners.add("water_ambient", new JsonArray());
        spawners.add("water_creature", new JsonArray());
        return spawners;
    }

    private static JsonArray arrayOf(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject entry(String type, int weight, int minCount, int maxCount) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", type);
        entry.addProperty("maxCount", maxCount);
        entry.addProperty("minCount", minCount);
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonArray arrayOfSpawner(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        return array;
    }
}
