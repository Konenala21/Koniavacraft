package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAGIC_ORE_KEY = registerKey("magic_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MANA_BLOOM_PATCH_KEY = registerKey("mana_bloom_patch");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        List<OreConfiguration.TargetBlockState> magicOreTargets = List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.MAGIC_ORE.get().defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), ModBlocks.DEEPSLATE_MAGIC_ORE.get().defaultBlockState())
        );

        register(context, MAGIC_ORE_KEY, Feature.ORE, new OreConfiguration(magicOreTargets, 15));

        // 魔力花方塊配置 - 只能放在草地上，不會取代草
        Holder<ConfiguredFeature<?, ?>> manaBloomBlock = Holder.direct(new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.MANA_BLOOM.get().defaultBlockState()))));

        // ✅ 性能優化：使用單一 REPLACEABLE 標籤檢查，避免重複檢查
        // AIR 已經包含在 REPLACEABLE 標籤中，無需單獨檢查
        Holder<PlacedFeature> manaBloomPlaced = Holder.direct(new PlacedFeature(
                manaBloomBlock,
                List.of(
                        // ✅ 優化：只檢查 REPLACEABLE 標籤（已包含空氣）
                        BlockPredicateFilter.forPredicate(
                                BlockPredicate.matchesTag(Vec3i.ZERO, BlockTags.REPLACEABLE)
                        ),
                        // ✅ 檢查下方方塊是否適合放置花朵（草地、泥土等）
                        PlacementUtils.filteredByBlockSurvival(ModBlocks.MANA_BLOOM.get())
                )
        ));
        // 調整密度：減少嘗試次數和散佈範圍
        // tries: 32 -> 8 (減少 75%)
        // xz_spread: 6 -> 4 (減少水平散佈)
        // y_spread: 2 -> 1 (減少垂直散佈)
        RandomPatchConfiguration manaBloomPatch = new RandomPatchConfiguration(8, 4, 1, manaBloomPlaced);
        register(context, MANA_BLOOM_PATCH_KEY, Feature.RANDOM_PATCH, manaBloomPatch);
    }


    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register
            (BootstrapContext<ConfiguredFeature<?, ?>> context,ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
