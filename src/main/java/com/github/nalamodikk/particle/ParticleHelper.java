package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.examples.GuidedFlowOptions;
import com.github.nalamodikk.particle.examples.MagicCircleOptions;
import com.github.nalamodikk.particle.network.SpawnMagicCirclePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * ??????????? *
 * ????芬謘??API ??????????殉???? */
public class ParticleHelper {

    /**
     * ??踐赤?擗??選?????啾???????(?叟垓??殉????
     */
    public static void createManaGeneratorEffect(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 1);
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * ???蹓?????謒?(?叟垓??殉????
     */
    public static void createPersistentCircle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnMagicCirclePacket packet = new SpawnMagicCirclePacket(pos, 2);
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, serverLevel.getChunkAt(pos).getPos(), packet);
        }
    }

    /**
     * ???獢?????啾???????(3D ???????
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
     * ??????啾????(??擗釭擐梁??)
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
                dx * 0.1, dy * 0.1, dz * 0.1, // ?輯撒??delta ????豲??賹撞???
                1 // speed
            );
        }
    }
}
