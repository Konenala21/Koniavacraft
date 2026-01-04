// 🔧 通用方塊替換工具類 - 優化版
package com.github.nalamodikk.common.utils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap; // 🚀 新增這個import
import java.util.function.BiPredicate;

/**
 * 🔧 通用方塊替換工具 - 高性能版
 */
public class BlockReplacementUtils {

    // 🚀 優化：緩存生物群系檢查結果
    private static final Map<ChunkAccess, ResourceKey<Biome>> BIOME_CACHE = new WeakHashMap<>();

    /**
     * 🎯 核心方法：替換指定生物群系的方塊
     */
    public static int replace(ServerLevel level, ChunkAccess chunk,
                              ResourceKey<Biome> targetBiome, Map<Block, Block> replacements) {
        return replace(level, chunk, targetBiome, replacements, Map.of());
    }

    /**
     * 🎯 核心方法：替換指定生物群系的方塊（帶條件）- 優化版
     */
    public static int replace(ServerLevel level, ChunkAccess chunk,
                              ResourceKey<Biome> targetBiome,
                              Map<Block, Block> replacements,
                              Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions) {

        // 🚀 優化1：快速生物群系檢查
        if (!isBiomeFast(level, chunk, targetBiome)) {
            return 0;
        }

        // 🚀 優化2：智能範圍掃描
        return performSmartReplacement(level, chunk, replacements, conditions);
    }

    // 🚀 新增：公開的快速生物群系檢查方法
    public static boolean isBiomeFast(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> targetBiome) {
        // 檢查緩存
        ResourceKey<Biome> cachedBiome = BIOME_CACHE.get(chunk);
        if (cachedBiome != null) {
            return cachedBiome.equals(targetBiome);
        }

        // 多點採樣檢查，而不是只檢查中心點
        int[][] samplePoints = {
                {4, 4}, {12, 4}, {4, 12}, {12, 12}, {8, 8}
        };

        int matchCount = 0;
        for (int[] point : samplePoints) {
            BlockPos pos = new BlockPos(
                    chunk.getPos().x * 16 + point[0],
                    level.getSeaLevel(),
                    chunk.getPos().z * 16 + point[1]
            );

            if (level.getBiome(pos).is(targetBiome)) {
                matchCount++;
            }
        }

        // 如果大部分點都匹配，認為是目標生物群系
        boolean isTargetBiome = matchCount >= 3;

        if (isTargetBiome) {
            BIOME_CACHE.put(chunk, targetBiome);
        }

        return isTargetBiome;
    }

    /**
     * 🌟 便捷方法：快速創建替換規則
     */
    public static Map<Block, Block> rules(Block source, Block target) {
        return Map.of(source, target);
    }

    public static Map<Block, Block> rules(Block s1, Block t1, Block s2, Block t2) {
        return Map.of(s1, t1, s2, t2);
    }

    public static Map<Block, Block> rules(Block s1, Block t1, Block s2, Block t2, Block s3, Block t3) {
        return Map.of(s1, t1, s2, t2, s3, t3);
    }

    /**
     * 🌟 便捷方法：創建條件規則
     */
    public static Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions(
            Block source, BiPredicate<ChunkAccess, BlockPos> condition) {
        return Map.of(source, condition);
    }

    /**
     * 🚀 智能替換：減少掃描範圍 + 早期退出
     */
    private static int performSmartReplacement(ServerLevel level, ChunkAccess chunk,
                                               Map<Block, Block> replacements,
                                               Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int replacedCount = 0;

        // 🚀 優化：縮小掃描範圍
        int seaLevel = level.getSeaLevel();
        int minY = Math.max(seaLevel - 5, level.getMinBuildHeight()); // 減少到5格
        int maxY = Math.min(seaLevel + 5, level.getMaxBuildHeight());   // 減少到5格

        // 🚀 優化：跳躍掃描 - 不是每個方塊都檢查
        for (int x = 0; x < 16; x += 2) {  // 每2格檢查一次
            for (int z = 0; z < 16; z += 2) {
                for (int y = maxY; y >= minY; y--) {
                    pos.set(
                            chunk.getPos().x * 16 + x,
                            y,
                            chunk.getPos().z * 16 + z
                    );

                    BlockState currentState = chunk.getBlockState(pos);
                    Block currentBlock = currentState.getBlock();

                    // 🚀 優化：如果當前方塊不在替換列表中，跳過
                    if (!replacements.containsKey(currentBlock) && !conditions.containsKey(currentBlock)) {
                        continue;
                    }

                    // 🎯 檢查條件替換
                    if (conditions.containsKey(currentBlock)) {
                        BiPredicate<ChunkAccess, BlockPos> condition = conditions.get(currentBlock);
                        if (condition.test(chunk, pos)) {
                            Block targetBlock = replacements.get(currentBlock);
                            if (targetBlock != null) {
                                // 🚀 優化：替換周圍的相同方塊
                                replacedCount += replaceCluster(chunk, pos, currentBlock, targetBlock);
                                continue;
                            }
                        }
                    }

                    // 🔄 檢查簡單替換
                    if (replacements.containsKey(currentBlock)) {
                        Block targetBlock = replacements.get(currentBlock);
                        // 🚀 優化：替換周圍的相同方塊
                        replacedCount += replaceCluster(chunk, pos, currentBlock, targetBlock);
                    }
                }
            }
        }

        return replacedCount;
    }

    /**
     * 🚀 新功能：聚類替換 - 發現一個目標方塊時，替換周圍相同的方塊
     */
    private static int replaceCluster(ChunkAccess chunk, BlockPos centerPos, Block sourceBlock, Block targetBlock) {
        int replaced = 0;

        // 替換3x3範圍內相同的方塊
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centerPos.offset(dx, 0, dz);

                // 確保在chunk範圍內
                if (pos.getX() >= chunk.getPos().x * 16 &&
                        pos.getX() < chunk.getPos().x * 16 + 16 &&
                        pos.getZ() >= chunk.getPos().z * 16 &&
                        pos.getZ() < chunk.getPos().z * 16 + 16) {

                    if (chunk.getBlockState(pos).getBlock() == sourceBlock) {
                        chunk.setBlockState(pos, targetBlock.defaultBlockState(), false);
                        replaced++;
                    }
                }
            }
        }

        return replaced;
    }

    // === 建造者類保持不變 ===
    public static class ReplacementBuilder {
        private final Map<Block, Block> replacements = new HashMap<>();
        private final Map<Block, BiPredicate<ChunkAccess, BlockPos>> conditions = new HashMap<>();

        public ReplacementBuilder replace(Block source, Block target) {
            replacements.put(source, target);
            return this;
        }

        public ReplacementBuilder replaceIf(Block source, Block target,
                                            BiPredicate<ChunkAccess, BlockPos> condition) {
            replacements.put(source, target);
            conditions.put(source, condition);
            return this;
        }

        public int apply(ServerLevel level, ChunkAccess chunk, ResourceKey<Biome> targetBiome) {
            return BlockReplacementUtils.replace(level, chunk, targetBiome, replacements, conditions);
        }
    }

    // === 優化的常用條件 ===

    /**
     * 🌊 優化的深層地下檢查
     */
    public static BiPredicate<ChunkAccess, BlockPos> DEEP_UNDERGROUND = (chunk, pos) -> {
        // 🚀 優化：減少檢查範圍
        int solidBlocks = 0;
        for (int y = pos.getY() + 1; y <= pos.getY() + 3; y++) { // 減少到3格
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!chunk.getBlockState(checkPos).isAir()) {
                solidBlocks++;
            }
        }
        return solidBlocks >= 2; // 降低標準
    };

    /**
     * 🌱 地表檢查
     */
    public static BiPredicate<ChunkAccess, BlockPos> SURFACE_ONLY = DEEP_UNDERGROUND.negate();

    /**
     * 🎲 隨機機率（緩存優化）
     */
    public static BiPredicate<ChunkAccess, BlockPos> chance(float probability) {
        return (chunk, pos) -> {
            // 使用位置作為種子，確保結果可重現
            long seed = pos.asLong();
            return new java.util.Random(seed).nextFloat() < probability;
        };
    }

    /**
     * 🌊 靠近水源（優化版）
     */
    public static BiPredicate<ChunkAccess, BlockPos> nearWater(int radius) {
        return (chunk, pos) -> {
            // 🚀 優化：減少檢查點
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    BlockPos checkPos = pos.offset(dx, 0, dz);
                    if (chunk.getBlockState(checkPos).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    // 🧹 清理緩存
    public static void clearCache() {
        BIOME_CACHE.clear();
    }
}