package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

public final class ManaConcentrationHelper {

    private static float cached = 0.10f;
    private static int ticks = 0;
    private static final int INTERVAL = 40;

    private ManaConcentrationHelper() {}

    public static float getConcentration() { return cached; }

    public static void reset() { cached = 0.10f; ticks = 0; }

    public static void clientTick(Player player) {
        if (++ticks < INTERVAL) return;
        ticks = 0;
        cached = calculate(player);
    }

    private static float calculate(Player player) {
        if (!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) return cached;
        BlockPos pos = player.blockPosition();
        float base = getBiomeBase(level, pos);
        float bonus = getMachineBonus(level, pos);
        return Mth.clamp(base + bonus, 0f, 1f);
    }

    private static float getBiomeBase(LevelReader level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(ModBiomes.MANA_PLAINS))       return 0.80f;
        if (biome.is(BiomeTags.IS_NETHER))          return 0.00f;
        if (biome.is(BiomeTags.IS_END))             return 0.03f;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN))      return 0.06f;
        if (biome.is(BiomeTags.IS_OCEAN))           return 0.07f;
        if (biome.is(BiomeTags.IS_JUNGLE))          return 0.15f;
        if (biome.is(BiomeTags.IS_FOREST))          return 0.12f;
        if (biome.is(BiomeTags.IS_TAIGA))           return 0.11f;
        if (biome.is(BiomeTags.IS_SAVANNA))         return 0.06f;
        if (biome.is(BiomeTags.IS_BADLANDS))        return 0.03f;
        if (biome.is(BiomeTags.IS_BEACH))           return 0.08f;
        return 0.10f;
    }

    private static float getMachineBonus(net.minecraft.client.multiplayer.ClientLevel level, BlockPos playerPos) {
        int count = 0;
        double rangeSq = 32.0 * 32.0;
        int cx0 = (playerPos.getX() - 32) >> 4;
        int cx1 = (playerPos.getX() + 32) >> 4;
        int cz0 = (playerPos.getZ() - 32) >> 4;
        int cz1 = (playerPos.getZ() + 32) >> 4;

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getKey().distSqr(playerPos) > rangeSq) continue;
                    ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entry.getValue().getType());
                    if (id != null && KoniavacraftMod.MOD_ID.equals(id.getNamespace())) count++;
                }
            }
        }
        return Math.min(0.35f, count * 0.005f);
    }
}
