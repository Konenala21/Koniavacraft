package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.network.SpawnMagicCirclePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 粒子效果助手類
 *
 * 提供簡單的 API 來生成各種粒子效果
 */
public class ParticleHelper {

    /**
     * 在發電機位置創建魔法陣效果
     *
     * 通過網絡包通知客戶端生成旋轉的粒子魔法陣
     */
    public static void createManaGeneratorEffect(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            // 發送網絡包到附近的玩家
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 1); // effectType 1 = manaGenerator
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * 創建持久性魔法陣（不會自動消失）
     */
    public static void createPersistentCircle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 2); // effectType 2 = persistent
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * 創建簡單的發光粒子圓環（使用原版粒子 - 臨時方案）
     */
    public static void createSimpleCircle(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int particleCount = 30;
        float radius = 0.8f;

        for (int i = 0; i < particleCount; i++) {
            float angle = (float)(2 * Math.PI * i / particleCount);
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() + 0.1;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            // 使用原版發光粒子作為臨時展示
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.GLOW,
                x, y, z,
                1, 0, 0, 0, 0
            );
        }
    }
}
