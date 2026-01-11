package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.examples.GuidedFlowOptions;
import com.github.nalamodikk.particle.examples.MagicCircleOptions;
import com.github.nalamodikk.particle.network.SpawnMagicCirclePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 粒子效果助手類
 *
 * 提供簡單的 API 來生成各種粒子效果
 */
public class ParticleHelper {

    /**
     * 在發電機位置創建魔法陣效果 (多粒子組成)
     */
    public static void createManaGeneratorEffect(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 1);
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * 創建持久性魔法陣 (多粒子組成)
     */
    public static void createPersistentCircle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 2);
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * 創建單一旋轉魔法陣粒子 (3D 旋轉演示)
     */
    public static void createSingleRotatingCircle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            MagicCircleOptions options = new MagicCircleOptions(1.0f, 0xFFFFFF, 1.0f);
            
            serverLevel.sendParticles(
                options,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                1, // count
                0, 0, 0, // delta
                0 // speed
            );
        }
    }

    /**
     * 創建導向魔力流 (路徑追蹤演示)
     */
    public static void createGuidedFlow(Level level, BlockPos startPos, BlockPos endPos) {
        if (level instanceof ServerLevel serverLevel) {
            double dx = endPos.getX() - startPos.getX();
            double dy = endPos.getY() - startPos.getY();
            double dz = endPos.getZ() - startPos.getZ();
            
            GuidedFlowOptions options = new GuidedFlowOptions(0.2f, 0x3BBAF7, 1.0f);
            
            serverLevel.sendParticles(
                options,
                startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5,
                1,
                dx * 0.1, dy * 0.1, dz * 0.1, // 使用 delta 傳遞初始速度向量
                1 // speed
            );
        }
    }
}
