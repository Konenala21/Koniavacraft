package com.github.nalamodikk.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {

    public static final ResourceKey<Level> VOID_MIRROR = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror")
    );

    public static final ResourceKey<DimensionType> VOID_MIRROR_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror")
    );

    public static final ResourceKey<Biome> VOID_MIRROR_BIOME = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror")
    );

    public static final ResourceKey<LevelStem> VOID_MIRROR_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror")
    );

    public static final ResourceLocation VOID_MIRROR_EFFECTS =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "void_mirror");
}
