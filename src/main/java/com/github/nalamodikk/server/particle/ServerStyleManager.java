package com.github.nalamodikk.server.particle;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.network.packet.ParticleStylePayload;
import com.github.nalamodikk.particle.control.ControlType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class ServerStyleManager {

    public static void spawnStyle(ServerLevel level, Vec3 pos, String styleType) {
        UUID uuid = UUID.randomUUID();
        
        // 廣播給附近玩家
        ParticleStylePayload packet = new ParticleStylePayload(
            uuid, ControlType.CREATE, styleType, pos, 0.0f, 1.0f
        );
        
        // 使用 NeoForge 的 PacketDistributor
        PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(pos)), packet);
        
        KoniavacraftMod.LOGGER.info("Server spawned style {} at {}", styleType, pos);
    }
}