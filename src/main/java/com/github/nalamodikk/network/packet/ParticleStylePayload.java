package com.github.nalamodikk.network.packet;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.control.ControlType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 粒子樣式同步封包
 * 
 * 用於伺服器通知客戶端創建、更新或移除粒子樣式
 */
public record ParticleStylePayload(
    UUID uuid,
    ControlType controlType,
    String styleTypeId, // 用於識別是哪種 Style (如 "roma_magic")
    Vec3 pos,
    float rotation,
    float scale
) implements CustomPacketPayload {

    public static final Type<ParticleStylePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "particle_style"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleStylePayload> STREAM_CODEC = StreamCodec.composite(
        net.minecraft.core.UUIDUtil.STREAM_CODEC, ParticleStylePayload::uuid,
        net.minecraft.network.codec.ByteBufCodecs.INT.map(ControlType::getTypeById, ControlType::getId), ParticleStylePayload::controlType,
        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ParticleStylePayload::styleTypeId,
        com.github.nalamodikk.network.codec.Vec3StreamCodec.INSTANCE, ParticleStylePayload::pos,
        net.minecraft.network.codec.ByteBufCodecs.FLOAT, ParticleStylePayload::rotation,
        net.minecraft.network.codec.ByteBufCodecs.FLOAT, ParticleStylePayload::scale,
        ParticleStylePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}