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

    public static final ResourceKey<Level> SPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space")
    );

    public static final ResourceKey<DimensionType> SPACE_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space")
    );

    public static final ResourceKey<Biome> SPACE_BIOME = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space")
    );

    public static final ResourceKey<LevelStem> SPACE_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space")
    );

    public static final ResourceLocation SPACE_EFFECTS =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "space");

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

    // ── 月球維度（第一顆可降落的行星）────────────────────────────────────
    public static final ResourceKey<Level> MOON = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "moon")
    );

    public static final ResourceKey<DimensionType> MOON_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "moon")
    );

    public static final ResourceKey<Biome> MOON_BIOME = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "moon")
    );

    public static final ResourceKey<LevelStem> MOON_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "moon")
    );

    public static final ResourceLocation MOON_EFFECTS =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "moon");

    // ── 飛船影子維度（純後台：contraption 方塊真正存放+tick 的隱藏世界，玩家永不進入）────
    public static final ResourceKey<Level> SHIP_SHADOW = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_shadow")
    );

    public static final ResourceKey<DimensionType> SHIP_SHADOW_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_shadow")
    );

    public static final ResourceKey<Biome> SHIP_SHADOW_BIOME = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_shadow")
    );

    public static final ResourceKey<LevelStem> SHIP_SHADOW_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ship_shadow")
    );
}
