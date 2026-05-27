package com.github.nalamodikk.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.concurrent.CompletableFuture;

public class BoundedFlatChunkGenerator extends FlatLevelSource {

    public static final int HALF_SIZE = 250;

    public static final MapCodec<BoundedFlatChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings)
        ).apply(instance, BoundedFlatChunkGenerator::new)
    );

    public BoundedFlatChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private static boolean isInside(int x, int z) {
        return Math.abs(x) <= HALF_SIZE && Math.abs(z) <= HALF_SIZE;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int endX = startX + 15;
        int endZ = startZ + 15;

        if (!isInside(startX, startZ) && !isInside(endX, endZ)
                && !isInside(startX, endZ) && !isInside(endX, startZ)) {
            return CompletableFuture.completedFuture(chunk);
        }

        return super.fillFromNoise(blender, randomState, structureManager, chunk).thenApply(filled -> {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int minY = filled.getMinBuildHeight();
            int maxY = filled.getMaxBuildHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (isInside(startX + x, startZ + z)) continue;
                    for (int y = minY; y < maxY; y++) {
                        pos.set(x, y, z);
                        filled.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
            return filled;
        });
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return isInside(x, z) ? super.getBaseHeight(x, z, type, level, random) : level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return isInside(x, z) ? super.getBaseColumn(x, z, level, random) : new NoiseColumn(level.getMinBuildHeight(), new net.minecraft.world.level.block.state.BlockState[0]);
    }
}
