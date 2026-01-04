package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

public class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_MAGIC_ORE = registerKey("add_magic_ore");
    public static final ResourceKey<BiomeModifier> ADD_MANA_BLOOM = registerKey("add_mana_bloom");
    private static final TagKey<Biome> MANA_BLOOM_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "has_mana_bloom"));

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);
        context.register(ADD_MAGIC_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(placedFeatures.getOrThrow(ModPlacedFeatures.MAGIC_ORE_PLACED_KEY))),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        context.register(ADD_MANA_BLOOM, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(MANA_BLOOM_BIOMES),
                HolderSet.direct(List.of(placedFeatures.getOrThrow(ModPlacedFeatures.MANA_BLOOM_PLACED_KEY))),
                GenerationStep.Decoration.VEGETAL_DECORATION
        ));

    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name));
    }
}
