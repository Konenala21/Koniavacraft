package com.github.nalamodikk.dimension;

import com.github.nalamodikk.register.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;

/**
 * 月球地形生成器：低頻丘陵（月海起伏）+ 散布隕石坑（碗狀凹陷 + 凸緣）。
 * 純程序，用 world seed 決定地貌。
 */
public class MoonChunkGenerator extends ChunkGenerator {

    public static final MapCodec<MoonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
        ).apply(instance, MoonChunkGenerator::new)
    );

    private static final int BASE_HEIGHT = 70;   // 平均地表高度
    private static final int HILL_AMP    = 14;   // 丘陵起伏振幅
    private static final int CRATER_GRID = 64;   // 隕石坑分布網格

    // 實心結構：月壤(表層) → 月岩 → 深層月岩 → 發光核心 [minY, CORE_TOP]
    public static final int CRUST_BOTTOM = 0;    // 地表下沉判定門檻（低於此視為地底，不畫星空）
    public static final int CORE_TOP     = -40;  // 發光核心頂，往下到世界底都是核心

    private final BlockState regolith;
    private final BlockState moonStone;
    private final BlockState deepStone;
    private final BlockState core;
    private final BlockState air;

    public MoonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.regolith  = ModBlocks.MOON_REGOLITH.get().defaultBlockState();
        this.moonStone = ModBlocks.MOON_STONE.get().defaultBlockState();
        this.deepStone = ModBlocks.MOON_DEEPSTONE.get().defaultBlockState();
        this.core      = ModBlocks.MOON_CORE.get().defaultBlockState();
        this.air       = Blocks.AIR.defaultBlockState();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    // ── 噪聲 ──────────────────────────────────────────────────────────────
    private static float hash2(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0x7fffffff) / (float)0x7fffffff;
    }

    private static float valueNoise(float x, float z) {
        int xi = (int)Math.floor(x), zi = (int)Math.floor(z);
        float xf = x - xi, zf = z - zi;
        float u = xf*xf*(3-2*xf), v = zf*zf*(3-2*zf);
        float a = hash2(xi, zi),     b = hash2(xi+1, zi);
        float c = hash2(xi, zi+1),   d = hash2(xi+1, zi+1);
        return (a*(1-u)+b*u)*(1-v) + (c*(1-u)+d*u)*v;
    }

    private static float fbm(float x, float z) {
        float v = 0, amp = 0.5f, freq = 1;
        for (int i = 0; i < 4; i++) {
            v += amp * valueNoise(x*freq, z*freq);
            freq *= 2; amp *= 0.5f;
        }
        return v;
    }

    /** 該 (x,z) 的地表高度：丘陵 - 隕石坑深度 + 坑緣。 */
    private int surfaceHeight(int x, int z) {
        // 丘陵
        float hills = (fbm(x / 90f, z / 90f) - 0.5f) * 2f * HILL_AMP;
        float h = BASE_HEIGHT + hills;

        // 隕石坑：找所在網格 + 鄰格最近的坑中心
        int gx = Math.floorDiv(x, CRATER_GRID), gz = Math.floorDiv(z, CRATER_GRID);
        float crater = 0f;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int cgx = gx + ox, cgz = gz + oz;
                float r1 = hash2(cgx, cgz);
                if (r1 < 0.45f) continue; // 多數網格無坑
                // 坑中心（網格內隨機）
                float cxF = (cgx + hash2(cgx*7+1, cgz)) * CRATER_GRID;
                float czF = (cgz + hash2(cgx, cgz*7+1)) * CRATER_GRID;
                float radius = 10f + hash2(cgx+3, cgz+5) * 22f; // 10~32
                float d = (float)Math.hypot(x - cxF, z - czF);
                if (d > radius * 1.25f) continue;
                float depth = radius * 0.45f;
                if (d < radius) {
                    // 碗狀（拋物線）凹陷
                    float t = d / radius;
                    crater -= depth * (1 - t*t);
                } else {
                    // 坑緣凸起（噴出物）
                    float t = (d - radius) / (radius * 0.25f);
                    crater += depth * 0.18f * (1 - t);
                }
            }
        }
        return Math.round(h + crater);
    }

    /** 該 Y 在實心月球的方塊：月壤/月岩/深層月岩/核心。top 為該柱地表高度。 */
    private BlockState blockAt(int y, int top, int minY) {
        if (y > top)        return air;       // 地表以上
        if (y > top - 4)    return regolith;  // 表層月壤
        if (y <= CORE_TOP)  return core;      // 底部發光核心（到世界底）
        if (y > top - 28)   return moonStone; // 中層月岩
        return deepStone;                     // 深層月岩
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
                                                        net.minecraft.world.level.StructureManager structures,
                                                        ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = startX + dx, wz = startZ + dz;
                int top = surfaceHeight(wx, wz);
                for (int y = minY; y <= top; y++) {
                    BlockState state = blockAt(y, top, minY);
                    if (state == air) continue; // 中空不放方塊
                    pos.set(dx, y, dz);
                    chunk.setBlockState(pos, state, false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, net.minecraft.world.level.StructureManager structures,
                             RandomState random, ChunkAccess chunk) {
        // 地表已在 fillFromNoise 處理
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager,
                             net.minecraft.world.level.StructureManager structures, ChunkAccess chunk,
                             GenerationStep.Carving step) { }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) { }

    @Override
    public int getGenDepth() { return 384; }

    @Override
    public int getSeaLevel() { return 0; }

    @Override
    public int getMinY() { return -64; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return surfaceHeight(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int top = surfaceHeight(x, z);
        int minY = level.getMinBuildHeight();
        BlockState[] col = new BlockState[level.getHeight()];
        for (int i = 0; i < col.length; i++) {
            col[i] = blockAt(minY + i, top, minY);
        }
        return new NoiseColumn(minY, col);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> info, RandomState random, BlockPos pos) {
        info.add("Moon terrain: hills + craters");
    }
}
